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

import java.util.LinkedList;

import org.matsim.core.api.experimental.events.EventsManager;

/**
 * The message factory is used for creating and disposing messages - mainly for
 * performance gain to have lesser garbage collection.
 * 
 * @author rashid_waraich
 */
public class MessageFactory {

	private final EventsManager eventsManager;
	private LinkedList<EndLegMessage> endLegMessageQueue = new LinkedList<EndLegMessage>();
	private LinkedList<EnterRoadMessage> enterRoadMessageQueue = new LinkedList<EnterRoadMessage>();
	private LinkedList<StartingLegMessage> startingLegMessageQueue = new LinkedList<StartingLegMessage>();
	private LinkedList<LeaveRoadMessage> leaveRoadMessageQueue = new LinkedList<LeaveRoadMessage>();
	private LinkedList<EndRoadMessage> endRoadMessageQueue = new LinkedList<EndRoadMessage>();

	private LinkedList<DeadlockPreventionMessage> deadlockPreventionMessageQueue = new LinkedList<DeadlockPreventionMessage>();

	public MessageFactory(EventsManager eventsManager) {
		this.eventsManager = eventsManager;
	}

	public void disposeEndLegMessage(EndLegMessage message) {
		if (!JDEQSimConfigGroup.isGC_MESSAGES()) {
			endLegMessageQueue.add(message);
		}
	}

	public void disposeEnterRoadMessage(EnterRoadMessage message) {
		if (!JDEQSimConfigGroup.isGC_MESSAGES()) {
			enterRoadMessageQueue.add(message);
		}
	}

	public void disposeStartingLegMessage(StartingLegMessage message) {
		if (!JDEQSimConfigGroup.isGC_MESSAGES()) {
			startingLegMessageQueue.add(message);
		}
	}

	public void disposeLeaveRoadMessage(LeaveRoadMessage message) {
		if (!JDEQSimConfigGroup.isGC_MESSAGES()) {
			leaveRoadMessageQueue.add(message);
		}
	}

	public void disposeEndRoadMessage(EndRoadMessage message) {
		if (!JDEQSimConfigGroup.isGC_MESSAGES()) {
			endRoadMessageQueue.add(message);
		}
	}

	public void disposeDeadlockPreventionMessage(DeadlockPreventionMessage message) {
		if (!JDEQSimConfigGroup.isGC_MESSAGES()) {
			deadlockPreventionMessageQueue.add(message);
		}
	}

	public EndLegMessage getEndLegMessage(Scheduler scheduler, Vehicle vehicle) {
		if (endLegMessageQueue.size() == 0) {
			return new EndLegMessage(scheduler, vehicle, eventsManager);
		} else {
			EndLegMessage message = endLegMessageQueue.poll();
			message.resetMessage(scheduler, vehicle);
			return message;
		}
	}

	public EnterRoadMessage getEnterRoadMessage(Scheduler scheduler, Vehicle vehicle) {
		if (enterRoadMessageQueue.size() == 0) {
			return new EnterRoadMessage(scheduler, vehicle, eventsManager);
		} else {
			EnterRoadMessage message = enterRoadMessageQueue.poll();
			message.resetMessage(scheduler, vehicle);
			return message;
		}
	}

	public StartingLegMessage getStartingLegMessage(Scheduler scheduler, Vehicle vehicle) {
		if (startingLegMessageQueue.size() == 0) {
			return new StartingLegMessage(scheduler, vehicle, eventsManager);
		} else {
			StartingLegMessage message = startingLegMessageQueue.poll();
			message.resetMessage(scheduler, vehicle);
			return message;
		}
	}

	public LeaveRoadMessage getLeaveRoadMessage(Scheduler scheduler, Vehicle vehicle) {
		if (leaveRoadMessageQueue.size() == 0) {
			return new LeaveRoadMessage(scheduler, vehicle, eventsManager);
		} else {
			LeaveRoadMessage message = leaveRoadMessageQueue.poll();
			message.resetMessage(scheduler, vehicle);
			return message;
		}
	}

	public EndRoadMessage getEndRoadMessage(Scheduler scheduler, Vehicle vehicle) {
		if (endRoadMessageQueue.size() == 0) {
			return new EndRoadMessage(scheduler, vehicle, eventsManager);
		} else {
			EndRoadMessage message = endRoadMessageQueue.poll();
			message.resetMessage(scheduler, vehicle);
			return message;
		}
	}

	public DeadlockPreventionMessage getDeadlockPreventionMessage(Scheduler scheduler, Vehicle vehicle) {
		if (deadlockPreventionMessageQueue.size() == 0) {
			return new DeadlockPreventionMessage(scheduler, vehicle, eventsManager);
		} else {
			DeadlockPreventionMessage message = deadlockPreventionMessageQueue.poll();
			message.resetMessage(scheduler, vehicle);
			return message;
		}
	}

	public void GC_ALL_MESSAGES() {
		endLegMessageQueue = new LinkedList<EndLegMessage>();
		enterRoadMessageQueue = new LinkedList<EnterRoadMessage>();
		startingLegMessageQueue = new LinkedList<StartingLegMessage>();
		leaveRoadMessageQueue = new LinkedList<LeaveRoadMessage>();
		endRoadMessageQueue = new LinkedList<EndRoadMessage>();

		deadlockPreventionMessageQueue = new LinkedList<DeadlockPreventionMessage>();
	}

	public LinkedList<EndLegMessage> getEndLegMessageQueue() {
		return endLegMessageQueue;
	}

	public LinkedList<EnterRoadMessage> getEnterRoadMessageQueue() {
		return enterRoadMessageQueue;
	}

	public LinkedList<StartingLegMessage> getStartingLegMessageQueue() {
		return startingLegMessageQueue;
	}

	public LinkedList<LeaveRoadMessage> getLeaveRoadMessageQueue() {
		return leaveRoadMessageQueue;
	}

	public LinkedList<EndRoadMessage> getEndRoadMessageQueue() {
		return endRoadMessageQueue;
	}

	public LinkedList<DeadlockPreventionMessage> getDeadlockPreventionMessageQueue() {
		return deadlockPreventionMessageQueue;
	}

	public EventsManager getEventsManager() {
		return eventsManager;
	}
}
