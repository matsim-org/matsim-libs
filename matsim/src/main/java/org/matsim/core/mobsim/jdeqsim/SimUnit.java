/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.jdeqsim;

/**
 * The basic building block for all simulation units.
 *
 * @author rashid_waraich
 */
public abstract class SimUnit {

	protected Scheduler scheduler = null;

	public SimUnit(Scheduler scheduler) {
		this.scheduler = scheduler;
	}

	public void sendMessage(Message m, SimUnit targetUnit, double messageArrivalTime) {
		m.setSendingUnit(this);
		m.setReceivingUnit(targetUnit);
		m.setMessageArrivalTime(messageArrivalTime);
		scheduler.schedule(m);
	}

	public Scheduler getScheduler() {
		return scheduler;
	}
}
