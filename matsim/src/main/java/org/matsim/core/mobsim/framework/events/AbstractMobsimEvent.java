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
package org.matsim.core.mobsim.framework.events;

import org.matsim.core.mobsim.framework.Mobsim;

/**
 * An abstract superclass for all classes implementing the
 * QueueSimulationEvent interface.
 *
 * @author dgrether
 */
abstract class AbstractMobsimEvent<T extends Mobsim> {
	
	private final T queuesim;

	AbstractMobsimEvent(T queuesim){
		this.queuesim = queuesim;
	}

	public T getQueueSimulation() {
		return this.queuesim;
	}

}
