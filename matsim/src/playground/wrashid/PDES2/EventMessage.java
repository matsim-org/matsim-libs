package playground.wrashid.PDES2;

import org.matsim.events.BasicEvent;
import org.matsim.events.AgentDepartureEvent;

public abstract class EventMessage extends SelfhandleMessage {
	Vehicle vehicle;
	String eventType="";
	Scheduler scheduler;
	boolean logMessage=true;
	
	public EventMessage(Scheduler scheduler,Vehicle vehicle) {
		super();
		this.vehicle = vehicle;
		this.scheduler=scheduler;
		assert(this.vehicle!=null);
	}
	
	public abstract void logEvent();
	
	public void printMessageLogString() {
		/*
		if (logMessage){
			EventLog ev=new EventLog(this.getMessageArrivalTime(),Integer.parseInt(vehicle.getOwnerPerson().getId().toString()),vehicle.getLegIndex()-1,Integer.parseInt(vehicle.getCurrentLink().getId().toString()),Integer.parseInt(vehicle.getCurrentLink().getFromNode().getId().toString()),Integer.parseInt(vehicle.getCurrentLink().getToNode().getId().toString()),eventType);
			SimulationParameters.eventOutputLog.add(ev);
			if (SimulationParameters.debugMode){
				ev.print();
			}
		}
		*/
		logEvent();
	}

	public void resetMessage(Scheduler scheduler,Vehicle vehicle){
		this.scheduler=scheduler;
		this.vehicle=vehicle;
		//messageId=getMessageCounterAndIncrement();
		assert(this.vehicle!=null);
		isAcrossBorderMessage=false;
		reviveMessage();
	}
	
	

}
