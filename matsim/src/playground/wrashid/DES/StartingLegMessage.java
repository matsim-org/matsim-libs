package playground.wrashid.DES;

import java.util.ArrayList;

import org.matsim.network.Link;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Plan;

public class StartingLegMessage extends EventMessage {

	public StartingLegMessage(Scheduler scheduler,Vehicle vehicle) {
		super(scheduler,vehicle);
		eventType=SimulationParameters.START_LEG;
	}

	@Override
	public void selfhandleMessage() {
		
		// attempt to enter street.
		Road road=Road.allRoads.get(vehicle.getCurrentLink().getId().toString());
		road.enterRequest(vehicle);
	}
	
	
}
