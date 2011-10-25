/* *********************************************************************** *
 * project: org.matsim.*
 * UnexistingAttributeException.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.agentsmating.logitbasedmating.framework;

/**
 * Thrown when an attribute cannot be found.
 *
 * @author thibautd
 */
public class UnexistingAttributeException extends Exception {
	private static final long serialVersionUID = 1L;

	public UnexistingAttributeException() {
		super();
	}

	public UnexistingAttributeException(final String msg) {
		super(msg);
	}

	public UnexistingAttributeException(final String msg, final Throwable cause) {
		super(msg, cause);
	}

	public UnexistingAttributeException(final Throwable cause) {
		super(cause);
	}
}


