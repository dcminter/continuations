package com.paperstack.continuation;

import java.util.Optional;

public class Result<T> {
	private final Optional<T> value;
	private final Optional<Throwable> throwable;
	
	public Result(final T value) {
		this.value = Optional.of(value);
		this.throwable = Optional.empty();
	}
	
	public Result(final Throwable throwable) {
		this.value = Optional.empty();
		this.throwable = Optional.of(throwable);
	}
	
	public boolean isOk() {
		return value.isPresent();
	}
	
	public T getValue() throws Throwable {
		return value.orElse(null);
	}
	
	public Throwable getThrowable() {
		return throwable.orElse(null);
	}
}
