/* *********************************************************************** *
 * project: org.matsim.*
 * QueueSimulationBeforeSimStepEventImpl
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.core.mobsim.queuesim.events;

import org.matsim.core.mobsim.framework.Simulation;


/**
 * @author dgrether
 */
public class SimulationBeforeSimStepEventImpl<T extends Simulation> extends AbstractSimulationEvent<T>  
	implements SimulationBeforeSimStepEvent<T> {

	private double time;

	public SimulationBeforeSimStepEventImpl(T queuesim, double time) {
		super(queuesim);
		this.time = time;
	}

	/**
	 * @see org.matsim.core.mobsim.queuesim.events.SimulationBeforeSimStepEvent#getSimulationTime()
	 */
	public double getSimulationTime() {
		return this.time;
	}
}
