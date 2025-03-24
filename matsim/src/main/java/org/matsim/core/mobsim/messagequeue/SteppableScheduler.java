
/* *********************************************************************** *
 * project: org.matsim.*
 * SteppableScheduler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

import org.matsim.core.mobsim.framework.Steppable;

import jakarta.inject.Inject;

public class SteppableScheduler extends Scheduler implements Steppable {

	private Message lookahead;
	private boolean finished = false;

	@Inject
	public SteppableScheduler(MessageQueue queue) {
		super(queue);
	}

	@Override
	public void doSimStep(double time) {
		finished = false; // I don't think we can restart once the queue has run dry, but just in case.

		// "lookahead" is, I think, just a cache of the next message in the queue, to avoid having to retreive it again.
		// yyyy looks like a potential bug to me if some other message gets inserted with an earlier message arrival time?  kai, feb'19
		// yes, I also think this works only if all messages are known in advance. marcel, march 2025
		if (lookahead != null && time < lookahead.getMessageArrivalTime()) {
			return;
		}
		if (lookahead != null) {
			lookahead.handleMessage();
			lookahead = null;
		}
		while (!queue.isEmpty()) {
			Message m = queue.getNextMessage();
			if (m != null && m.getMessageArrivalTime() <= time) {
				m.handleMessage();
			} else {
				lookahead = m;
				return;
			}
		}
		finished = true; // queue has run dry.
	}

	public boolean isFinished() {
		return finished;
	}

}
