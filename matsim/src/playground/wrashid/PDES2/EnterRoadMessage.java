package playground.wrashid.PDES2;

import org.matsim.events.BasicEvent;
import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.LinkEnterEvent;
import org.matsim.network.Link;
import org.matsim.population.Leg;

public class EnterRoadMessage extends EventMessage {



	@Override
	public void selfhandleMessage() {
		//Ask road to really enter the road
		// => Road will then let us enter the road and tell us, when we can leave the road.
		//System.out.println("enter road message");
		// enter the road and find out the time for leaving the street
		
		
		
		Road road=Road.allRoads.get(vehicle.getCurrentLink().getId().toString());
		synchronized (road){
			road.simTime=messageArrivalTime;
			road.enterRoad(vehicle);
		}
		
		//System.out.println("enter road: " + road.getLink().getId() + "; vehicle: " + vehicle.getOwnerPerson().getId());
		
	}
	
	public EnterRoadMessage(Scheduler scheduler,Vehicle vehicle) {
		super(scheduler,vehicle);
		eventType=SimulationParameters.ENTER_LINK;
	}

	public void logEvent() {
		BasicEvent event=null;
		
		if (eventType.equalsIgnoreCase(SimulationParameters.ENTER_LINK)){
			event=new LinkEnterEvent(this.getMessageArrivalTime(),vehicle.getOwnerPerson().getId().toString(),vehicle.getCurrentLink().getId().toString(),vehicle.getLegIndex()-1);
			//event=new LinkEnterEvent(this.getMessageArrivalTime(),vehicle.getOwnerPerson(),vehicle.getCurrentLink(),vehicle.getCurrentLeg());
		}
		
		//SimulationParameters.events.processEvent(event);
		//SimulationParameters.processEvent(event);
		SimulationParameters.bufferEvent(event);
	}

	
	
}
