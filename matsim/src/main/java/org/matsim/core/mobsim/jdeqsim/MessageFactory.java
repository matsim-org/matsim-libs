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

/**
 * The message factory is used for creating and disposing messages - mainly for
 * performance gain to have lesser garbage collection.
 * 
 * @author rashid_waraich
 */
public class MessageFactory {

	private static LinkedList<EndLegMessage> endLegMessageQueue = new LinkedList<EndLegMessage>();
	private static LinkedList<EnterRoadMessage> enterRoadMessageQueue = new LinkedList<EnterRoadMessage>();
	private static LinkedList<StartingLegMessage> startingLegMessageQueue = new LinkedList<StartingLegMessage>();
	private static LinkedList<LeaveRoadMessage> leaveRoadMessageQueue = new LinkedList<LeaveRoadMessage>();
	private static LinkedList<EndRoadMessage> endRoadMessageQueue = new LinkedList<EndRoadMessage>();

	private static LinkedList<DeadlockPreventionMessage> deadlockPreventionMessageQueue = new LinkedList<DeadlockPreventionMessage>();

	public static void disposeEndLegMessage(EndLegMessage message) {
		if (!JDEQSimConfigGroup.isGC_MESSAGES()) {
			endLegMessageQueue.add(message);
		}
	}

	public static void disposeEnterRoadMessage(EnterRoadMessage message) {
		if (!JDEQSimConfigGroup.isGC_MESSAGES()) {
			enterRoadMessageQueue.add(message);
		}
	}

	public static void disposeStartingLegMessage(StartingLegMessage message) {
		if (!JDEQSimConfigGroup.isGC_MESSAGES()) {
			startingLegMessageQueue.add(message);
		}
	}

	public static void disposeLeaveRoadMessage(LeaveRoadMessage message) {
		if (!JDEQSimConfigGroup.isGC_MESSAGES()) {
			leaveRoadMessageQueue.add(message);
		}
	}

	public static void disposeEndRoadMessage(EndRoadMessage message) {
		if (!JDEQSimConfigGroup.isGC_MESSAGES()) {
			endRoadMessageQueue.add(message);
		}
	}

	public static void disposeDeadlockPreventionMessage(DeadlockPreventionMessage message) {
		if (!JDEQSimConfigGroup.isGC_MESSAGES()) {
			deadlockPreventionMessageQueue.add(message);
		}
	}

	public static EndLegMessage getEndLegMessage(Scheduler scheduler, Vehicle vehicle) {
		if (endLegMessageQueue.size() == 0) {
			return new EndLegMessage(scheduler, vehicle);
		} else {
			EndLegMessage message = endLegMessageQueue.poll();
			message.resetMessage(scheduler, vehicle);
			return message;
		}
	}

	public static EnterRoadMessage getEnterRoadMessage(Scheduler scheduler, Vehicle vehicle) {
		if (enterRoadMessageQueue.size() == 0) {
			return new EnterRoadMessage(scheduler, vehicle);
		} else {
			EnterRoadMessage message = enterRoadMessageQueue.poll();
			message.resetMessage(scheduler, vehicle);
			return message;
		}
	}

	public static StartingLegMessage getStartingLegMessage(Scheduler scheduler, Vehicle vehicle) {
		if (startingLegMessageQueue.size() == 0) {
			return new StartingLegMessage(scheduler, vehicle);
		} else {
			StartingLegMessage message = startingLegMessageQueue.poll();
			message.resetMessage(scheduler, vehicle);
			return message;
		}
	}

	public static LeaveRoadMessage getLeaveRoadMessage(Scheduler scheduler, Vehicle vehicle) {
		if (leaveRoadMessageQueue.size() == 0) {
			return new LeaveRoadMessage(scheduler, vehicle);
		} else {
			LeaveRoadMessage message = leaveRoadMessageQueue.poll();
			message.resetMessage(scheduler, vehicle);
			return message;
		}
	}

	public static EndRoadMessage getEndRoadMessage(Scheduler scheduler, Vehicle vehicle) {
		if (endRoadMessageQueue.size() == 0) {
			return new EndRoadMessage(scheduler, vehicle);
		} else {
			EndRoadMessage message = endRoadMessageQueue.poll();
			message.resetMessage(scheduler, vehicle);
			return message;
		}
	}

	public static DeadlockPreventionMessage getDeadlockPreventionMessage(Scheduler scheduler, Vehicle vehicle) {
		if (deadlockPreventionMessageQueue.size() == 0) {
			return new DeadlockPreventionMessage(scheduler, vehicle);
		} else {
			DeadlockPreventionMessage message = deadlockPreventionMessageQueue.poll();
			message.resetMessage(scheduler, vehicle);
			return message;
		}
	}

	public static void GC_ALL_MESSAGES() {
		endLegMessageQueue = new LinkedList<EndLegMessage>();
		enterRoadMessageQueue = new LinkedList<EnterRoadMessage>();
		startingLegMessageQueue = new LinkedList<StartingLegMessage>();
		leaveRoadMessageQueue = new LinkedList<LeaveRoadMessage>();
		endRoadMessageQueue = new LinkedList<EndRoadMessage>();

		deadlockPreventionMessageQueue = new LinkedList<DeadlockPreventionMessage>();
	}

	public static LinkedList<EndLegMessage> getEndLegMessageQueue() {
		return endLegMessageQueue;
	}

	public static LinkedList<EnterRoadMessage> getEnterRoadMessageQueue() {
		return enterRoadMessageQueue;
	}

	public static LinkedList<StartingLegMessage> getStartingLegMessageQueue() {
		return startingLegMessageQueue;
	}

	public static LinkedList<LeaveRoadMessage> getLeaveRoadMessageQueue() {
		return leaveRoadMessageQueue;
	}

	public static LinkedList<EndRoadMessage> getEndRoadMessageQueue() {
		return endRoadMessageQueue;
	}

	public static LinkedList<DeadlockPreventionMessage> getDeadlockPreventionMessageQueue() {
		return deadlockPreventionMessageQueue;
	}

}
