package com.paperstack.continuation;

import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.SynchronousQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a Continuation. Derived classes should over-ride the run method
 * and NOT return from it. Results are provided to the invoking class by
 * invoking the yield method with the appropriate result entry.
 *
 * The implementation works by using a Thread object to represent the context of
 * the continuation. The thread is suspended upon yield, and resumed when the
 * continuation is re-invoked. By implementing Iterable the continuation can be
 * used in an extended-for loop.
 *
 * Don't use this code for anything. It's got lots of problems, not least the
 * overhead of the thread (and its stack space) associated with every Continuation
 * object!
 *
 * @author Dave Minter
 */
abstract public class Continuation<T> implements Iterable<T> {

	private static final Logger LOGGER = LoggerFactory.getLogger(Continuation.class);

	private static final String NAME = Continuation.class.getCanonicalName();

	// The thread which will be used to retained to represent
	// this Continuation's context
	private final Thread thread = new Thread(this::threadRun, NAME);

	private void threadRun() {
		try {
			LOGGER.debug("Run of continuations thread");
			Continuation.this.run();
		} catch (final Throwable t) {
			LOGGER.debug("Run failed with exception (to be wrapped)", t);
			throw new ContinuationException(t);
		}
	}

	// If an exception is thrown on the thread, assign it here for later retrieval
	private Throwable thrown = null;

	// Used to synchronize consumption of the output of the continuation
	// with production of values
	private final SynchronousQueue<Optional<T>> results = new SynchronousQueue<>();

	/**
	 * Provided to allow iteration over the continuation
	 */
	final public Iterator<T> iterator() {
		return new Iterator<T>() {
			public void remove() {
				throw new UnsupportedOperationException();
			}

			public boolean hasNext() {
				return true;
			}

			public T next() {
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

	/**
	 * Provided for invocation by the extending class in place of the normal return
	 * statement of a conventional method.
	 * 
	 * @param item The item to yield up to the consumer
	 * @throws InterruptedException
	 */
	final protected void yield(final T item) throws InterruptedException {
		results.put(Optional.of(item));
	}

	/**
	 * Allows direct invocation of the Continuation
	 * 
	 * @return The next value
	 * @throws Any exceptions that occurred within a continuation
	 */
	final public T next() throws Throwable {
		initThread();

		final var taken = results.take();

		synchronized(this) {
			if (thrown != null) {
				throw thrown;
			}
		}

		return taken.orElse(null);
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
		
		synchronized(this) {
			this.thrown = throwable;
		}
		
		try {
			this.results.put(Optional.empty());
		} catch (final InterruptedException e) {
			LOGGER.error("Unexpected interruption when populating the continuation response", e);
		}
	}

	/**
	 * The producer method. Instead of returning, this method should make calls to
	 * yield()
	 * 
	 * @throws Throwable
	 */
	abstract public void run() throws Throwable;
}