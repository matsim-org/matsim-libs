package playground.wrashid.PDES2;

import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.BasicEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.LinkLeaveEvent;

public class LeaveRoadMessage extends EventMessage {

	@Override
	public void selfhandleMessage() {
		//System.out.println("leave road message");
		Road road=(Road)this.receivingUnit;
		road.simTime=messageArrivalTime;
		road.leaveRoad(vehicle);
		
		//System.out.println("leave road: " + road.getLink().getId() + "; vehicle: " + vehicle.getOwnerPerson().getId());
		
	}

	public LeaveRoadMessage(Scheduler scheduler,Vehicle vehicle) {
		super(scheduler,vehicle);
		eventType=SimulationParameters.LEAVE_LINK;
	}
	
	public void printMessageLogString() {
		/*
		Road road=(Road)this.receivingUnit;
		
		if (logMessage){
			EventLog ev=new EventLog(this.getMessageArrivalTime(),Integer.parseInt(vehicle.getOwnerPerson().getId().toString()),vehicle.getLegIndex()-1,Integer.parseInt(road.getLink().getId().toString()),Integer.parseInt(vehicle.getCurrentLink().getFromNode().getId().toString()),Integer.parseInt(vehicle.getCurrentLink().getToNode().getId().toString()),eventType);
			SimulationParameters.eventOutputLog.add(ev);
			if (SimulationParameters.debugMode){
				ev.print();
			}
		}
		*/
		logEvent();
	}

	@Override
	public void logEvent() {
		Road road=(Road)this.receivingUnit;
		BasicEvent event=null;
		
		if (eventType.equalsIgnoreCase(SimulationParameters.LEAVE_LINK)){
			event=new LinkLeaveEvent(this.getMessageArrivalTime(),vehicle.getOwnerPerson(),vehicle.getCurrentLink(),vehicle.getCurrentLeg());
		}
		
		//SimulationParameters.events.processEvent(event);
		//SimulationParameters.processEvent(event);
		SimulationParameters.bufferEvent(event);
	}

	
	
}
