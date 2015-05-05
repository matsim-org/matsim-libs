/* *********************************************************************** *
 * project: org.matsim.*
 * ControlerSetupIterationEvent.java
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

import org.matsim.core.controler.Controler;


/**
 * ControlerEvent class to notify all observers interested in the preparation of an iteration
 *
 * @author dgrether
 */
public final class IterationStartsEvent extends ControlerEvent {

	/**
	 * the number of the iteration
	 */
	private final int iteration;

	public IterationStartsEvent(final Controler controler, final int iteration) {
		super(controler);
		this.iteration = iteration;
	}

	/**
	 *
	 * @return the number of the iteration to setup
	 */
	public int getIteration() {
		return this.iteration;
	}

}
