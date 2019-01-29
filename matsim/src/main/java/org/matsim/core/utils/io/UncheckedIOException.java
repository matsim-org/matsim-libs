package org.matsim.core.utils.io;

/**
 * A simple (unchecked) exception class to typically wrap IOExceptions.
 * 
 * @author mrieser
 */
public class UncheckedIOException extends RuntimeException {

	public UncheckedIOException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public UncheckedIOException(final Throwable cause) {
		super(cause);
	}
	
	public UncheckedIOException(final String message) {
		super(message);
	}
}
