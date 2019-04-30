package com.paperstack.continuation;

import com.paperstack.continuation.Continuation;

public class Fibonacci extends Continuation<Integer> {
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