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
package org.matsim.core.mobsim.framework.events;

/**
 * Events is broadcasted before a simulation step in the Mobsim starts.
 * 
 * @author dgrether, shoerl
 */
public class MobsimBeforeSimStepEvent implements AbstractMobsimEvent {

	private final double time;

	public MobsimBeforeSimStepEvent(double time) {
		this.time = time;
	}

	public double getSimulationTime() {
		return this.time;
	}
}
