package com.paperstack.continuation;

import static java.lang.String.format;

public class ContinuationException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public ContinuationException(final Throwable thrown) {
		super(format("Continuation interrupted by '%s'", thrown.getMessage()), thrown);
	}
}