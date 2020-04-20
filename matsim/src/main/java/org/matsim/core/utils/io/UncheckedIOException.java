
/* *********************************************************************** *
 * project: org.matsim.*
 * UncheckedIOException.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

 package org.matsim.core.utils.io;

/**
 * A simple (unchecked) exception class to typically wrap IOExceptions.
 * 
 * @author mrieser
 */
public class UncheckedIOException extends RuntimeException {
	private static final long serialVersionUID = 6186620063027481864L;

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
