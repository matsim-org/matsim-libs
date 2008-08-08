package playground.wrashid.PDES1;

import java.util.concurrent.ConcurrentLinkedQueue;

public class MessageFactory {
	
	private static ConcurrentLinkedQueue<EndLegMessage> endLegMessageQueue=new ConcurrentLinkedQueue<EndLegMessage>();
	private static ConcurrentLinkedQueue<EnterRoadMessage> enterRoadMessageQueue=new ConcurrentLinkedQueue<EnterRoadMessage>();
	private static ConcurrentLinkedQueue<StartingLegMessage> startingLegMessageQueue=new ConcurrentLinkedQueue<StartingLegMessage>();
	private static ConcurrentLinkedQueue<LeaveRoadMessage> leaveRoadMessageQueue=new ConcurrentLinkedQueue<LeaveRoadMessage>();
	private static ConcurrentLinkedQueue<EndRoadMessage> endRoadMessage=new ConcurrentLinkedQueue<EndRoadMessage>();
	
	private static ConcurrentLinkedQueue<DeadlockPreventionMessage> deadlockPreventionMessageQueue=new ConcurrentLinkedQueue<DeadlockPreventionMessage>();
	
	private static ConcurrentLinkedQueue<NullMessage> nullMessageQueue=new ConcurrentLinkedQueue<NullMessage>();
	private static ConcurrentLinkedQueue<EnterRequestMessage> enterRequestMessageQueue=new ConcurrentLinkedQueue<EnterRequestMessage>();
	
	
	public static void disposeNullMessage(NullMessage message){
		if (nullMessageQueue.size()<SimulationParameters.maxQueueLength){
			nullMessageQueue.add(message);
		}
	}
	
	public static void disposeEnterRequestMessage(EnterRequestMessage message){
		if (enterRequestMessageQueue.size()<SimulationParameters.maxQueueLength){
			enterRequestMessageQueue.add(message);
		}
	}
	
	
	
	public static void disposeEndLegMessage(EndLegMessage message){
		if (endLegMessageQueue.size()<SimulationParameters.maxQueueLength){
			endLegMessageQueue.add(message);
		}
	}
	
	public static void disposeEnterRoadMessage(EnterRoadMessage message){
		if (enterRoadMessageQueue.size()<SimulationParameters.maxQueueLength){
			enterRoadMessageQueue.add(message);
		}
	}
	
	public static void disposeStartingLegMessage(StartingLegMessage message){
		if (startingLegMessageQueue.size()<SimulationParameters.maxQueueLength){
			startingLegMessageQueue.add(message);
		}
	}
	
	public static void disposeLeaveRoadMessage(LeaveRoadMessage message){
		if (leaveRoadMessageQueue.size()<SimulationParameters.maxQueueLength){
			leaveRoadMessageQueue.add(message);
		}
	}
	
	public static void disposeEndRoadMessage(EndRoadMessage message){
		if (endRoadMessage.size()<SimulationParameters.maxQueueLength){
			endRoadMessage.add(message);
		}
	}
	
	public static void disposeDeadlockPreventionMessage(DeadlockPreventionMessage message){
		if (deadlockPreventionMessageQueue.size()<SimulationParameters.maxQueueLength){
			deadlockPreventionMessageQueue.add(message);
		}
	}


	public static EndLegMessage getEndLegMessage(Scheduler scheduler, Vehicle vehicle) {
		if (endLegMessageQueue.size()<SimulationParameters.minQueueLength){
			return new EndLegMessage(scheduler,vehicle);
		} else {
			EndLegMessage message=endLegMessageQueue.poll();
			message.resetMessage(scheduler, vehicle);
			return message;
		}
	}


	public static EnterRoadMessage getEnterRoadMessage(Scheduler scheduler,
			Vehicle vehicle) {
		if (enterRoadMessageQueue.size()<SimulationParameters.minQueueLength){
			return new EnterRoadMessage(scheduler,vehicle);
		} else {
			EnterRoadMessage message=enterRoadMessageQueue.poll();
			message.resetMessage(scheduler, vehicle);
			return message;
		}
	}


	public static StartingLegMessage getStartingLegMessage(Scheduler scheduler,
			Vehicle vehicle) {
		if (startingLegMessageQueue.size()<SimulationParameters.minQueueLength){
			return new StartingLegMessage(scheduler,vehicle);
		} else {
			StartingLegMessage message=startingLegMessageQueue.poll();
			message.resetMessage(scheduler, vehicle);
			return message;
		}
	}


	public static LeaveRoadMessage getLeaveRoadMessage(Scheduler scheduler,
			Vehicle vehicle) {
		if (leaveRoadMessageQueue.size()<SimulationParameters.minQueueLength){
			return new LeaveRoadMessage(scheduler,vehicle);
		} else {
			LeaveRoadMessage message=leaveRoadMessageQueue.poll();
			message.resetMessage(scheduler, vehicle);
			return message;
		}
	}


	public static EndRoadMessage getEndRoadMessage(Scheduler scheduler, Vehicle vehicle) {
		if (endRoadMessage.size()<SimulationParameters.minQueueLength){
			return new EndRoadMessage(scheduler,vehicle);
		} else {
			EndRoadMessage message=endRoadMessage.poll();
			message.resetMessage(scheduler, vehicle);
			return message;
		}
	}
	
	
	
	public static DeadlockPreventionMessage getDeadlockPreventionMessage(Scheduler scheduler,Vehicle vehicle){
		if (deadlockPreventionMessageQueue.size()<SimulationParameters.minQueueLength){
			return new DeadlockPreventionMessage(scheduler,vehicle);
		} else {
			DeadlockPreventionMessage message=deadlockPreventionMessageQueue.poll();
			message.resetMessage(scheduler, vehicle);
			return message;
		}
	}
	
	synchronized public static NullMessage getNullMessage(){
		//if (nullMessageQueue.size()<SimulationParameters.minQueueLength){
			return new NullMessage();
		//} else {
		//	return nullMessageQueue.poll();
		//}
	}
	
	
	public static EnterRequestMessage getEnterRequestMessage(Vehicle vehicle){
		if (enterRequestMessageQueue.size()<SimulationParameters.minQueueLength){
			return new EnterRequestMessage(vehicle);
		} else {
			return enterRequestMessageQueue.poll();
		}
	}
	
}
