/* *********************************************************************** *
 * project: org.matsim.*
 * ControlerScoringEvent.java.java
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
import org.matsim.core.replanning.ReplanningContext;

/**
 * Event class to notify observers that replanning should happen
 *
 * @author mrieser
 */
public class ReplanningEvent extends ControlerEvent {

	/**
	 * The iteration number
	 */
	private final int iteration;

	public ReplanningEvent(final Controler controler, final int iteration) {
		super(controler);
		this.iteration = iteration;
	}

	/**
	 * @return the number of the current iteration
	 */
	public int getIteration() {
		return this.iteration;
	}

	public ReplanningContext getReplanningContext() {
        return controler.getInjector().getInstance(ReplanningContext.class);
    }

}
