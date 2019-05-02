package com.paperstack.continuation;

import static java.lang.String.format;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.SynchronousQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContinuationBuilder {
	@SuppressWarnings("rawtypes")
	private static final Map<Thread,Continuation> CONTEXTS = new LinkedHashMap<>();
	
	public static <T> Iterable<T> build(final Callable<T> implementation) {
		final Continuation<T> continuation = new Continuation<>(implementation);
		CONTEXTS.put(continuation.getThread(),continuation);
		return continuation;
	}

	@SuppressWarnings({"rawtypes","unchecked"})
	public static <T> void yield(final T value) throws InterruptedException {
		final Continuation continuation = CONTEXTS.get(Thread.currentThread());
		continuation.yield(value);
	}

	private static final class Continuation<C> implements Iterable<C> {
		private static final Logger LOGGER = LoggerFactory.getLogger(Continuation.class);
		private final Thread thread;

		// Used to synchronize consumption of the output of the continuation
		// with production of values
		private final SynchronousQueue<Result<C>> results = new SynchronousQueue<>();
		private final Callable<C> implementation;

		public Continuation(final Callable<C> implementation) {
			this.thread = new Thread(this::threadRun, format("Continuation '%s'", implementation.getClass().getName()));
			this.implementation = implementation;
		}
		
		Thread getThread() {
			return thread;
		}

		private void threadRun() {
			try {
				LOGGER.debug("Run of continuations thread");
				implementation.call();
			} catch (final Throwable t) {
				LOGGER.debug("Run failed with exception (to be wrapped)", t);
				throw new ContinuationException(t);
			}
		}

		/**
		 * Provided for invocation by the extending class in place of the normal return
		 * statement of a conventional method.
		 * 
		 * @param item The item to yield up to the consumer
		 * @throws InterruptedException
		 */
		final void yield(final C item) throws InterruptedException {
			results.put(new Result<C>(item));
		}

		/**
		 * Allows direct invocation of the Continuation
		 * 
		 * @return The next value
		 * @throws Any exceptions that occurred within a continuation
		 */
		final public C next() throws Throwable {
			initThread();
			final var taken = results.take();
			if (!taken.isOk())
				throw taken.getThrowable();
			return taken.getValue();
		}

		// Initializes the thread representing the continuation's context.
		private void initThread() throws Throwable {
			if (!thread.isAlive()) {
				thread.setDaemon(true); // The JVM is allowed to exit if only the daemon thread is running
				thread.setUncaughtExceptionHandler(this::uncaughtExceptionHandler);
				thread.start();
			}
		}

		private void uncaughtExceptionHandler(final Thread thread, final Throwable throwable) {
			LOGGER.debug("Caught exception", throwable);
			try {
				this.results.put(new Result<C>(throwable));
			} catch (final InterruptedException e) {
				LOGGER.error("Unexpected interruption when populating the continuation response", e);
			}
		}

		/**
		 * Provided to allow iteration over the continuation
		 */
		final public Iterator<C> iterator() {
			return new Iterator<C>() {
				public void remove() {
					throw new UnsupportedOperationException();
				}

				public boolean hasNext() {
					return true;
				}

				public C next() {
					try {
						return Continuation.this.next();
					} catch (final ContinuationException e) {
						LOGGER.debug("Re-throwing wrapped exception", e);
						throw e;
					} catch (final Throwable t) {
						LOGGER.debug("Re-throwing and wrapping exception", t);
						throw new ContinuationException(t);
					}
				}
			};
		}
	}
}
