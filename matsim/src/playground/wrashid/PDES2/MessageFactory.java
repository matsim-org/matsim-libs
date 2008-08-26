package playground.wrashid.PDES2;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MessageFactory {

	private static LinkedList<EndLegMessage>[] endLegMessageQueue = new LinkedList[SimulationParameters.numberOfMessageExecutorThreads];
	private static LinkedList<EnterRoadMessage>[] enterRoadMessageQueue = new LinkedList[SimulationParameters.numberOfMessageExecutorThreads];
	private static LinkedList<StartingLegMessage>[] startingLegMessageQueue = new LinkedList[SimulationParameters.numberOfMessageExecutorThreads];
	private static LinkedList<LeaveRoadMessage>[] leaveRoadMessageQueue = new LinkedList[SimulationParameters.numberOfMessageExecutorThreads];
	private static LinkedList<EndRoadMessage>[] endRoadMessageQueue = new LinkedList[SimulationParameters.numberOfMessageExecutorThreads];

	private static LinkedList<DeadlockPreventionMessage>[] deadlockPreventionMessageQueue = new LinkedList[SimulationParameters.numberOfMessageExecutorThreads];

	private static LinkedList<ZoneBorderMessage>[] zoneBorderMessageQueue = new LinkedList[SimulationParameters.numberOfMessageExecutorThreads];
	private static LinkedList<EnterRequestMessage>[] enterRequestMessageQueue = new LinkedList[SimulationParameters.numberOfMessageExecutorThreads];
	private static LinkedList<TimerMessage>[] timerMessageQueue = new LinkedList[SimulationParameters.numberOfMessageExecutorThreads];

	private static int testCounter=0;
	
	public static int[] test_CompareCounter=new int[SimulationParameters.numberOfMessageExecutorThreads];
	
	static {
		for (int i = 0; i < SimulationParameters.numberOfMessageExecutorThreads; i++) {
			endLegMessageQueue[i] = new LinkedList<EndLegMessage>();
			enterRoadMessageQueue[i] = new LinkedList<EnterRoadMessage>();
			startingLegMessageQueue[i] = new LinkedList<StartingLegMessage>();
			leaveRoadMessageQueue[i] = new LinkedList<LeaveRoadMessage>();
			endRoadMessageQueue[i] = new LinkedList<EndRoadMessage>();
			deadlockPreventionMessageQueue[i] = new LinkedList<DeadlockPreventionMessage>();
			zoneBorderMessageQueue[i] = new LinkedList<ZoneBorderMessage>();
			enterRequestMessageQueue[i] = new LinkedList<EnterRequestMessage>();
			timerMessageQueue[i] = new LinkedList<TimerMessage>();
		}
	}

	// TODO: (perhaps) find out which messages are most frequent and then put
	// the right order here
	public static void dispose(Message message) {
		if (message instanceof EndLegMessage) {
			dispose(endLegMessageQueue[MessageExecutor.getThreadId()], message);
		} else if (message instanceof EnterRoadMessage) {
			dispose(enterRoadMessageQueue[MessageExecutor.getThreadId()],
					message);
		} else if (message instanceof StartingLegMessage) {
			dispose(startingLegMessageQueue[MessageExecutor.getThreadId()],
					message);
		} else if (message instanceof LeaveRoadMessage) {
			dispose(leaveRoadMessageQueue[MessageExecutor.getThreadId()],
					message);
		} else if (message instanceof EndRoadMessage) {
			dispose(endRoadMessageQueue[MessageExecutor.getThreadId()], message);
		} else if (message instanceof DeadlockPreventionMessage) {
			dispose(deadlockPreventionMessageQueue[MessageExecutor
					.getThreadId()], message);
		} else if (message instanceof ZoneBorderMessage) {
			dispose(zoneBorderMessageQueue[MessageExecutor.getThreadId()],
					message);
		} else if (message instanceof EnterRequestMessage) {
			dispose(enterRequestMessageQueue[MessageExecutor.getThreadId()],
					message);
		} else if (message instanceof TimerMessage) {
			dispose(timerMessageQueue[MessageExecutor.getThreadId()], message);
		} else {
			System.out.println("ERROR");
		}
	}

	private static void dispose(LinkedList list, Message message) {
		if (list.size() < SimulationParameters.maxQueueLength) {
			list.add(message);
		}
	}

	public static EndLegMessage getEndLegMessage(Scheduler scheduler,
			Vehicle vehicle) {
		if (endLegMessageQueue[MessageExecutor.getThreadId()].size() < SimulationParameters.minQueueLength) {
			return new EndLegMessage(scheduler, vehicle);
		} else {
			EndLegMessage message = endLegMessageQueue[MessageExecutor
					.getThreadId()].poll();
			message.resetMessage(scheduler, vehicle);
			return message;
		}
	}

	public static EnterRoadMessage getEnterRoadMessage(Scheduler scheduler,
			Vehicle vehicle) {
		test_CompareCounter[MessageExecutor.getThreadId()]++;
		if (enterRoadMessageQueue[MessageExecutor.getThreadId()].size() < SimulationParameters.minQueueLength) {
			return new EnterRoadMessage(scheduler, vehicle);
		} else {
			EnterRoadMessage message = enterRoadMessageQueue[MessageExecutor
					.getThreadId()].poll();
			message.resetMessage(scheduler, vehicle);
			return message;
		}
	}

	public static StartingLegMessage getStartingLegMessage(Scheduler scheduler,
			Vehicle vehicle) {
		try {
			if (startingLegMessageQueue[MessageExecutor.getThreadId()].size() < SimulationParameters.minQueueLength) {
				return new StartingLegMessage(scheduler, vehicle);
			} else {
				StartingLegMessage message = startingLegMessageQueue[MessageExecutor
						.getThreadId()].poll();
				message.resetMessage(scheduler, vehicle);
				return message;
			}
		} catch (Exception e) {
			return new StartingLegMessage(scheduler, vehicle);
		}
	}

	public static LeaveRoadMessage getLeaveRoadMessage(Scheduler scheduler,
			Vehicle vehicle) {
		if (leaveRoadMessageQueue[MessageExecutor.getThreadId()].size() < SimulationParameters.minQueueLength) {
			return new LeaveRoadMessage(scheduler, vehicle);
		} else {
			LeaveRoadMessage message = leaveRoadMessageQueue[MessageExecutor
					.getThreadId()].poll();
			message.resetMessage(scheduler, vehicle);
			return message;
		}
	}

	public static EndRoadMessage getEndRoadMessage(Scheduler scheduler,
			Vehicle vehicle) {
		if (endRoadMessageQueue[MessageExecutor.getThreadId()].size() < SimulationParameters.minQueueLength) {
			return new EndRoadMessage(scheduler, vehicle);
		} else {
			EndRoadMessage message = endRoadMessageQueue[MessageExecutor
					.getThreadId()].poll();
			message.resetMessage(scheduler, vehicle);
			return message;
		}
	}

	public static DeadlockPreventionMessage getDeadlockPreventionMessage(
			Scheduler scheduler, Vehicle vehicle) {
		if (deadlockPreventionMessageQueue[MessageExecutor.getThreadId()]
				.size() < SimulationParameters.minQueueLength) {
			return new DeadlockPreventionMessage(scheduler, vehicle);
		} else {
			DeadlockPreventionMessage message = deadlockPreventionMessageQueue[MessageExecutor
					.getThreadId()].poll();
			message.resetMessage(scheduler, vehicle);
			return message;
		}
	}

	public static ZoneBorderMessage getZoneBorderMessage() {
		try {
			if (zoneBorderMessageQueue[MessageExecutor.getThreadId()].size() < SimulationParameters.minQueueLength) {
				return new ZoneBorderMessage();
			} else {
				ZoneBorderMessage zbm=zoneBorderMessageQueue[MessageExecutor.getThreadId()]
				                     						.poll();
				zbm.reviveMessage();
				return zbm;
			}
		} catch (Exception e) {
			
			return new ZoneBorderMessage();
		}
	}

	public static EnterRequestMessage getEnterRequestMessage(
			Scheduler scheduler, Vehicle vehicle) {
		if (enterRequestMessageQueue[MessageExecutor.getThreadId()].size() < SimulationParameters.minQueueLength) {
			return new EnterRequestMessage(scheduler, vehicle);
		} else {
			EnterRequestMessage message = enterRequestMessageQueue[MessageExecutor
					.getThreadId()].poll();
			message.resetMessage(scheduler, vehicle);
			assert (message.vehicle != null);
			return message;
		}
	}

	public static TimerMessage getTimerMessage() {
		//testCounter++;
		//if (testCounter % 1000000==0){System.out.println(testCounter);}
		try {
			if (timerMessageQueue[MessageExecutor.getThreadId()].size() < SimulationParameters.minQueueLength) {
				return new TimerMessage();
			} else {
				TimerMessage tm=timerMessageQueue[MessageExecutor.getThreadId()].poll();
				tm.reviveMessage();
				return tm;
			}
		} catch (Exception e) {
			// this occurs only at the beginning, when no message executor
			// exists (invoked from main)
			return new TimerMessage();
		}
	}

}
