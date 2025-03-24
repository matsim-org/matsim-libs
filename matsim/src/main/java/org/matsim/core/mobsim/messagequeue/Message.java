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
 * The basic message type used in the micro-simulation.
 *
 * @author rashid_waraich
 */
public abstract class Message implements Comparable<Message> {

	private double messageArrivalTime = 0;

	public Message(double messageArrivalTime) {
		this.messageArrivalTime = messageArrivalTime;
	}

	public double getMessageArrivalTime() {
		return messageArrivalTime;
	}

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

}
