package com.paperstack.continuation;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import static com.paperstack.continuation.ContinuationBuilder.yield;
import com.paperstack.continuation.ContinuationBuilder;

public class ContinuationTest {

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	private static final int[] EXPECTED = { 0, 1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233, 377, 610, 987 };

	/**
	 * A continuation to calculate fibonacci numbers
	 */
	private final Iterable<Integer> fibonacciContinuation = ContinuationBuilder.build(() -> {
		yield(0);
		var i = 0;
		var j = 1;
		while (true) {
			yield(j);
			final var current = i + j;
			i = j;
			j = current;
		}			
	});

	/**
	 * A continuation to calculate Fibonacci numbers but blow up with an exception
	 * when 100 is exceeded.
	 */
	final Iterable<Integer> breakingFibonacciContinuation = ContinuationBuilder.build(() -> {
		yield(0);
		var i = 0;
		var j = 1;
		while (true) {
			if (j > 100) {
				throw new TestException("Kaboom!");
			}
			
			yield(j);
			final var current = i + j;
			i = j;
			j = current;
		}			
	});

	@Test
	public void testConsumedFibonacci() {
		var index = 0;		
		for (final int actual : fibonacciContinuation) {
			assertEquals(String.format("Fibonacci value incorrect at %d", index), actual, EXPECTED[index++]);
			if (index >= EXPECTED.length) {
				break;
			}
		}
	}

	@Test
	public void testConsumedFibonacciException() {
		exception.expect(ContinuationException.class);
		exception.expectCause(instanceOf(TestException.class));

		var index = 0;
		for (final int actual : breakingFibonacciContinuation) {
			assertEquals(String.format("Fibonacci value incorrect at %d", index), actual, EXPECTED[index++]);
			if (index >= EXPECTED.length) {
				break;
			}
		}
	}

	private static class TestException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public TestException(final String message) {
			super(message);
		}
	}
}
