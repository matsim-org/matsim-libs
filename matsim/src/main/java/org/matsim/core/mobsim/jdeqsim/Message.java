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

	protected static EventsManager eventsManager;

	public static void setEventsManager(EventsManager eventsManager) {
		Message.eventsManager = eventsManager;
	}

	private double messageArrivalTime = 0;
	private SimUnit sendingUnit;
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

	/**
	 * 
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
			return otherMessage.getPriority() - priority;
		}
	}

	public int getPriority() {
		return priority;
	}

	public SimUnit getSendingUnit() {
		return sendingUnit;
	}

	public void setSendingUnit(SimUnit sendingUnit) {
		this.sendingUnit = sendingUnit;
	}

	public SimUnit getReceivingUnit() {
		return receivingUnit;
	}

	public void setReceivingUnit(SimUnit receivingUnit) {
		this.receivingUnit = receivingUnit;
	}

	public abstract void handleMessage();

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
