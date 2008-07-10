package playground.wrashid.DES;

import java.util.ArrayList;

import org.matsim.events.BasicEvent;
import org.matsim.events.EventAgentArrival;
import org.matsim.events.EventAgentDeparture;
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
	
	public void logEvent() {
		BasicEvent event=null;
		
		if (eventType.equalsIgnoreCase(SimulationParameters.START_LEG)){
			event=new EventAgentDeparture(this.getMessageArrivalTime(),vehicle.getOwnerPerson().getId().toString(),vehicle.getLegIndex()-1,vehicle.getCurrentLink().getToNode().getId().toString());
		}
		
		SimulationParameters.events.processEvent(event);
	}
	
}
