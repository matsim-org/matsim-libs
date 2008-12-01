package playground.wrashid.DES;

import org.matsim.events.BasicEvent;
import org.matsim.events.AgentDepartureEvent;

public abstract class EventMessage extends Message {
	Vehicle vehicle;
	String eventType="";
	Scheduler scheduler;
	boolean logMessage=true;
	
	public EventMessage(Scheduler scheduler,Vehicle vehicle) {
		super();
		this.vehicle = vehicle;
		this.scheduler=scheduler;
	}


	public void resetMessage(Scheduler scheduler,Vehicle vehicle){
		this.scheduler=scheduler;
		this.vehicle=vehicle;
	}
	

}
