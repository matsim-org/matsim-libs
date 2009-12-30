/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractQueueSimulationEvent
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

import org.matsim.core.mobsim.Simulation;


/**
 * An abstract superclass for all classes implementing the
 * QueueSimulationEvent interface.
 * @author dgrether
 *
 */
public abstract class AbstractQueueSimulationEvent<T extends Simulation> implements
		QueueSimulationEvent<T> {
	
	private T queuesim;

	public AbstractQueueSimulationEvent(T queuesim){
		this.queuesim = queuesim;
	}
	
	/**
	 * @see org.matsim.core.mobsim.queuesim.events.QueueSimulationEvent#getQueueSimulation()
	 */
	public T getQueueSimulation() {
		return this.queuesim;
	}

}
