package playground.wrashid.DES;

import java.util.LinkedList;

public class MessageFactory {

	protected static LinkedList<EndLegMessage> endLegMessageQueue = new LinkedList<EndLegMessage>();
	protected static LinkedList<EnterRoadMessage> enterRoadMessageQueue = new LinkedList<EnterRoadMessage>();
	protected static LinkedList<StartingLegMessage> startingLegMessageQueue = new LinkedList<StartingLegMessage>();
	protected static LinkedList<LeaveRoadMessage> leaveRoadMessageQueue = new LinkedList<LeaveRoadMessage>();
	protected static LinkedList<EndRoadMessage> endRoadMessageQueue = new LinkedList<EndRoadMessage>();

	private static LinkedList<DeadlockPreventionMessage> deadlockPreventionMessageQueue = new LinkedList<DeadlockPreventionMessage>();

	public static void disposeEndLegMessage(EndLegMessage message) {
		if (!SimulationParameters.isGC_MESSAGES()) {
			endLegMessageQueue.add(message);
		}
	}

	public static void disposeEnterRoadMessage(EnterRoadMessage message) {
		if (!SimulationParameters.isGC_MESSAGES()) {
			enterRoadMessageQueue.add(message);
		}
	}

	public static void disposeStartingLegMessage(StartingLegMessage message) {
		if (!SimulationParameters.isGC_MESSAGES()) {
			startingLegMessageQueue.add(message);
		}
	}

	public static void disposeLeaveRoadMessage(LeaveRoadMessage message) {
		if (!SimulationParameters.isGC_MESSAGES()) {
			leaveRoadMessageQueue.add(message);
		}
	}

	public static void disposeEndRoadMessage(EndRoadMessage message) {
		if (!SimulationParameters.isGC_MESSAGES()) {
			endRoadMessageQueue.add(message);
		}
	}

	public static void disposeDeadlockPreventionMessage(
			DeadlockPreventionMessage message) {
		if (!SimulationParameters.isGC_MESSAGES()) {
			deadlockPreventionMessageQueue.add(message);
		}
	}

	public static EndLegMessage getEndLegMessage(Scheduler scheduler,
			Vehicle vehicle) {
		if (endLegMessageQueue.size() == 0) {
			return new EndLegMessage(scheduler, vehicle);
		} else {
			EndLegMessage message = endLegMessageQueue.poll();
			message.resetMessage(scheduler, vehicle);
			return message;
		}
	}

	public static EnterRoadMessage getEnterRoadMessage(Scheduler scheduler,
			Vehicle vehicle) {
		if (enterRoadMessageQueue.size() == 0) {
			return new EnterRoadMessage(scheduler, vehicle);
		} else {
			EnterRoadMessage message = enterRoadMessageQueue.poll();
			message.resetMessage(scheduler, vehicle);
			return message;
		}
	}

	public static StartingLegMessage getStartingLegMessage(Scheduler scheduler,
			Vehicle vehicle) {
		if (startingLegMessageQueue.size() == 0) {
			return new StartingLegMessage(scheduler, vehicle);
		} else {
			StartingLegMessage message = startingLegMessageQueue.poll();
			message.resetMessage(scheduler, vehicle);
			return message;
		}
	}

	public static LeaveRoadMessage getLeaveRoadMessage(Scheduler scheduler,
			Vehicle vehicle) {
		if (leaveRoadMessageQueue.size() == 0) {
			return new LeaveRoadMessage(scheduler, vehicle);
		} else {
			LeaveRoadMessage message = leaveRoadMessageQueue.poll();
			message.resetMessage(scheduler, vehicle);
			return message;
		}
	}

	public static EndRoadMessage getEndRoadMessage(Scheduler scheduler,
			Vehicle vehicle) {
		if (endRoadMessageQueue.size() == 0) {
			return new EndRoadMessage(scheduler, vehicle);
		} else {
			EndRoadMessage message = endRoadMessageQueue.poll();
			message.resetMessage(scheduler, vehicle);
			return message;
		}
	}

	public static DeadlockPreventionMessage getDeadlockPreventionMessage(
			Scheduler scheduler, Vehicle vehicle) {
		if (deadlockPreventionMessageQueue.size() == 0) {
			return new DeadlockPreventionMessage(scheduler, vehicle);
		} else {
			DeadlockPreventionMessage message = deadlockPreventionMessageQueue
					.poll();
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

}
