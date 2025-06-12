/* *********************************************************************** *
 * project: org.matsim.*
 * ControlerShutdownEvent.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.core.controler.events;

import org.matsim.core.controler.MatsimServices;

import java.util.Optional;

/**
 * ControlerEvent class to notify all observers of the Controler that it is shutdown
 *
 * @author dgrether
 */
public final class ShutdownEvent extends ControlerEvent {

	/**
	 * Flag to indicate if the controler was shutdown unexpected
	 */
	private final boolean unexpected;
	private final int iteration;
	private final Throwable exception;
	
	public ShutdownEvent(final MatsimServices controler, final boolean unexpected, int iteration, Throwable exception) {
		super(controler);
		this.unexpected = unexpected;
		this.iteration = iteration;
		this.exception = exception;
	}

	/**
	 * @return true if the  controler was shutdown unexpected, false if a normal shutdown occured
	 */
	public boolean isUnexpected() {
		return this.unexpected;
	}
	
	public int getIteration() {
		return iteration;
	}

	public Optional<Throwable> getException() {
		return Optional.ofNullable(this.exception);
	}
}
