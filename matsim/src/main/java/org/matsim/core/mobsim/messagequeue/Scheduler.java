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

package org.matsim.core.mobsim.messagequeue;

/**
 * The scheduler of the micro-simulation.
 *
 * @author rashid_waraich
 */
public class Scheduler {

	protected final MessageQueue queue;

	public Scheduler(MessageQueue queue) {
		this(queue, Double.MAX_VALUE);
	}

	public Scheduler(MessageQueue messageQueue, double simulationEndTime) {
		this.queue = messageQueue;
	}

	public void schedule(Message m) {
		queue.putMessage(m);
	}

}
