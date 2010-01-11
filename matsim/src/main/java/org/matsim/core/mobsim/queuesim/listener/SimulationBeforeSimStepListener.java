/* *********************************************************************** *
 * project: org.matsim.*
 * QueueSimulationBeforeSimStepListener
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
package org.matsim.core.mobsim.queuesim.listener;

import org.matsim.core.mobsim.framework.Simulation;
import org.matsim.core.mobsim.queuesim.events.SimulationBeforeSimStepEvent;


/**
 *  Listeners of QueueSimulation should implement this if they want to be 
 *  notified after QueueSimulation.beforeSimStep() was invoked.
 * @author dgrether
 *
 */
public interface SimulationBeforeSimStepListener<T extends Simulation> extends SimulationListener<T> {

	public void notifySimulationBeforeSimStep(SimulationBeforeSimStepEvent<T> e);
	
}
