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
 * The basic message type used in the micro-simulation.
 *
 * @author rashid_waraich
 */
public abstract class Message implements Comparable<Message> {

	private double messageArrivalTime = 0;

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
			return 0;
		}
	}

	public abstract void handleMessage();
	// yyyy we always seem to have "processEvent()" immediately followed by "handleMessage()", and it is not clear to me why we have both.  kai, feb'19
	// I think that the idea is that in "processEvent()" the normal MATSim event is generated and given to the eventsManager, while in handleMessage, everything else is done.

	public boolean isAlive() {
		return true;
	}

}
