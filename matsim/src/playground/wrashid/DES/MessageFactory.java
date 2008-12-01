package playground.wrashid.DES;

import java.util.concurrent.ConcurrentLinkedQueue;

public class MessageFactory {

	protected static ConcurrentLinkedQueue<EndLegMessage> endLegMessageQueue = new ConcurrentLinkedQueue<EndLegMessage>();
	protected static ConcurrentLinkedQueue<EnterRoadMessage> enterRoadMessageQueue = new ConcurrentLinkedQueue<EnterRoadMessage>();
	protected static ConcurrentLinkedQueue<StartingLegMessage> startingLegMessageQueue = new ConcurrentLinkedQueue<StartingLegMessage>();
	protected static ConcurrentLinkedQueue<LeaveRoadMessage> leaveRoadMessageQueue = new ConcurrentLinkedQueue<LeaveRoadMessage>();
	protected static ConcurrentLinkedQueue<EndRoadMessage> endRoadMessageQueue = new ConcurrentLinkedQueue<EndRoadMessage>();

	private static ConcurrentLinkedQueue<DeadlockPreventionMessage> deadlockPreventionMessageQueue = new ConcurrentLinkedQueue<DeadlockPreventionMessage>();

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
		endLegMessageQueue = new ConcurrentLinkedQueue<EndLegMessage>();
		enterRoadMessageQueue = new ConcurrentLinkedQueue<EnterRoadMessage>();
		startingLegMessageQueue = new ConcurrentLinkedQueue<StartingLegMessage>();
		leaveRoadMessageQueue = new ConcurrentLinkedQueue<LeaveRoadMessage>();
		endRoadMessageQueue = new ConcurrentLinkedQueue<EndRoadMessage>();

		deadlockPreventionMessageQueue = new ConcurrentLinkedQueue<DeadlockPreventionMessage>();
	}

}
