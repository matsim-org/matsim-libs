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

import java.util.PriorityQueue;

/**
 * The message queue of the micro-simulation.
 * <br/>
 * Via injection, one can currently get hold both of {@link MessageQueue} and {@link SteppableScheduler}.
 * Seems to me that the intended API is actually the latter??  But I don't actually know.  / kn
 *
 * @author rashid_waraich
 */
public class MessageQueue {
	private PriorityQueue<Message> queue = new PriorityQueue<>();

	/**
	 * Putting a message into the queue
	 */
	public void putMessage(Message m) {
		queue.add(m);
	}

	/**
	 * get the first message in the queue (with least time stamp)
	 */
	public Message getNextMessage() {
		return queue.poll();
	}

	public boolean isEmpty() {
		return queue.isEmpty();
	}

}
