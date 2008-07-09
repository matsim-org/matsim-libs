package playground.wrashid.DES;

import org.matsim.network.Link;
import org.matsim.plans.Leg;

public class EnterRoadMessage extends EventMessage {



	@Override
	public void selfhandleMessage() {
		//Ask road to really enter the road
		// => Road will then let us enter the road and tell us, when we can leave the road.

		// enter the road and find out the time for leaving the street
		
		Road road=Road.allRoads.get(vehicle.getCurrentLink().getId().toString());
		road.enterRoad(vehicle);
	}
	
	public EnterRoadMessage(Scheduler scheduler,Vehicle vehicle) {
		super(scheduler,vehicle);
		eventType=SimulationParameters.ENTER_LINK;
	}

}
