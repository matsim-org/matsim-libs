package playground.wrashid.DES;

import org.matsim.events.BasicEvent;
import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.LinkEnterEvent;
import org.matsim.network.Link;
import org.matsim.population.Leg;

public class EnterRoadMessage extends EventMessage {



	@Override
	public void handleMessage() {
		//Ask road to really enter the road
		// => Road will then let us enter the road and tell us, when we can leave the road.

		// enter the road and find out the time for leaving the street
		
		Road road=Road.allRoads.get(vehicle.getCurrentLink().getId().toString());
		road.enterRoad(vehicle);
	}
	
	public EnterRoadMessage(Scheduler scheduler,Vehicle vehicle) {
		super(scheduler,vehicle);
		eventType=SimulationParameters.ENTER_LINK;
		priority=SimulationParameters.PRIORITY_ENTER_ROAD_MESSAGE;
	}

	public void processEvent() {
		BasicEvent event=null;
		
		if (eventType.equalsIgnoreCase(SimulationParameters.ENTER_LINK)){
			event=new LinkEnterEvent(this.getMessageArrivalTime(),vehicle.getOwnerPerson().getId().toString(),vehicle.getCurrentLink().getId().toString(),vehicle.getLegIndex()-1);
		}
		
		SimulationParameters.events.processEvent(event);
	}
	
}
