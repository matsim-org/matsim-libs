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

import org.matsim.core.api.experimental.events.EventsManager;

/**
 * The basic message type used in the micro-simulation.
 *
 * @author rashid_waraich
 */
public abstract class Message implements Comparable<Message> {

	static EventsManager eventsManager;
	// yyyy we have bad experiences with static non-final stuff. kai, feb'19

	public static void setEventsManager(EventsManager eventsManager) {
		Message.eventsManager = eventsManager;
	}

	private double messageArrivalTime = 0;
//	private SimUnit sendingUnit;
	private SimUnit receivingUnit;
	protected int priority = 0;
	private boolean isAlive = true;

	public Message() {
	}

	public double getMessageArrivalTime() {
		return messageArrivalTime;
	}

	public void setMessageArrivalTime(double messageArrivalTime) {
		this.messageArrivalTime = messageArrivalTime;
	}

	public abstract void processEvent();
	// yyyy we always seem to have "processEvent()" immediately followed by "handleMessage()", and it is not clear to me why we have both.  kai, feb'19
	// I think that the idea is that in "processEvent()" the normal MATSim event is generated and given to the eventsManager, while in handleMessage, everything else is done.

	/**
	 * The comparison is done according to the message arrival Time. If the time
	 * is equal of two messages, then the priority of the messages is compared
	 */
	@Override
	public int compareTo(Message otherMessage) {
		if (messageArrivalTime > otherMessage.messageArrivalTime) {
			return 1;
		} else if (messageArrivalTime < otherMessage.messageArrivalTime) {
			return -1;
		} else {
			// higher priority means for a queue, that it comes first
			return otherMessage.priority - priority;
		}
	}

//	public int getPriority() {
//		return priority;
//	}
	// only needed internally.  kai, feb'19

//	public SimUnit getSendingUnit() {
//		return sendingUnit;
//	}
	// this method is only used once, as part of a test case, where it is only used to retreive the scheduler.  Which should, however, be the same if we retreive it from the
	// receiving unit.  kai, feb'19
	// replaced now by getReceivingUnit.getScheduler.  kai, feb'19

//	public void setSendingUnit(SimUnit sendingUnit) {
//		this.sendingUnit = sendingUnit;
//	}
	// sendingUnit never used/needed. kai, feb'19

	public SimUnit getReceivingUnit() {
		return receivingUnit;
	}

	public void setReceivingUnit(SimUnit receivingUnit) {
		// the receiving unit seems to be the object that one needs when handling the message.  I don't find this totally clear since maybe one would need two objects (such
		// as when they collide) or even more?  Then one needs to somehow find the other objects, indicating that one could find the first object through those methods as
		// well.  kai, feb'18
		this.receivingUnit = receivingUnit;
	}

	public abstract void handleMessage();
	// yyyy we always seem to have "processEvent()" immediately followed by "handleMessage()", and it is not clear to me why we have both.  kai, feb'19
	// I think that the idea is that in "processEvent()" the normal MATSim event is generated and given to the eventsManager, while in handleMessage, everything else is done.

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public void killMessage() {
		isAlive = false;
	}

	public void reviveMessage() {
		isAlive = true;
	}

	public boolean isAlive() {
		return isAlive;
	}

}
