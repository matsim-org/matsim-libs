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

import org.matsim.core.mobsim.qsim.jdeqsimengine.SteppableScheduler;

import java.util.PriorityQueue;

/**
 * The message queue of the micro-simulation.
 * <br/>
 * Via injection, one can currently get hold both of {@link MessageQueue} and {@link SteppableScheduler}.  Seems to me that the intendet API is actually the latter??  But I
 * don't actually know.
 *
 * @author rashid_waraich
 */
public class MessageQueue {
	private PriorityQueue<Message> queue1 = new PriorityQueue<Message>();
	private int queueSize = 0;

	/**
	 * 
	 * Putting a message into the queue
	 *
	 * @param m
	 */
	public void putMessage(Message m) {
		queue1.add(m);
		queueSize++;
	}

	/**
	 * 
	 * Remove the message from the queue and discard it. - queue1.remove(m) does
	 * not function, because it discards all message with the same priority as m
	 * from the queue. - This java api bug is reported at:
	 * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6207984
	 * 
	 * => queue1.removeAll(Collections.singletonList(m)); can be used, but it has
	 * been removed because of just putting a flag to kill a message is more efficient.
	 * 
	 * @param m
	 */
	public void removeMessage(Message m) {
		m.killMessage();
		queueSize--;
	}

	/**
	 * 
	 * get the first message in the queue (with least time stamp)
	 *
	 * @return
	 */
	public Message getNextMessage() {
		Message m = null;
		if (queue1.peek() != null) {
			// skip over dead messages
			while ((m = queue1.poll()) != null && !m.isAlive()) {

			}
			// only decrement, if message fetched
			if (m != null) {
				queueSize--;
			}
		}

		return m;
	}

	public boolean isEmpty() {
		return queue1.size() == 0;
	}

	public int getQueueSize() {
		return queueSize;
	}

}
