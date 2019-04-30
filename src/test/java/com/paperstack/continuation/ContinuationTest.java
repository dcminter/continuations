package com.paperstack.continuation;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ContinuationTest {

	@Test
	public void testFibonacciSequence() {
		final int[] expected = new int[] { 0, 1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233, 377, 610, 987 };

		int index = 0;
		for (final int actual : new Fibonacci()) {
			assertEquals(String.format("Fibonacci value incorrect at %d", index), actual, expected[index++]);
			if (index >= expected.length) {
				break;
			}
		}
	}

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	@Test
	public void testFibonacciSequenceException() {
		exception.expect(ContinuationException.class);
		exception.expectCause(instanceOf(TestException.class));

		final int[] expected = new int[] { 0, 1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233, 377, 610, 987 };

		int index = 0;
		for (final int actual : new BreakingFibonacci(100)) {
			assertEquals(String.format("Fibonacci value incorrect at %d", index), actual, expected[index++]);
			if (index >= expected.length) {
				break;
			}
		}
	}

	/**
	 * A continuation to calculate fibonacci numbers
	 */
	public static class Fibonacci extends Continuation<Integer> {
		public void run() throws Exception {
			yield(0);
			int i = 0;
			int j = 1;
			while (true) {
				yield(j);
				int current = i + j;
				i = j;
				j = current;
			}
		}
	}

	/**
	 * A continuation to calculate fibonacci numbers but blow up with an exception
	 * when the target value is exceeded.
	 */
	public static class BreakingFibonacci extends Continuation<Integer> {

		private final int target;

		public BreakingFibonacci(final int target) {
			this.target = target;
		}

		public void run() throws Exception {
			yield(0);
			int i = 0;
			int j = 1;
			while (true) {
				if (j > target) {
					throw new TestException("Kaboom!");
				}
				yield(j);
				int current = i + j;
				i = j;
				j = current;
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
