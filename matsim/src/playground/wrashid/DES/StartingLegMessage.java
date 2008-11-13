package playground.wrashid.DES;

import java.util.ArrayList;

import org.matsim.basic.v01.BasicLeg;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.BasicEvent;
import org.matsim.network.Link;
import org.matsim.population.Act;
import org.matsim.population.Plan;

public class StartingLegMessage extends EventMessage {

	public StartingLegMessage(Scheduler scheduler,Vehicle vehicle) {
		super(scheduler,vehicle);
		eventType=SimulationParameters.START_LEG;
	}

	@Override
	public void selfhandleMessage() {
		
		// attempt to enter street.
		
		if (vehicle.getCurrentLeg().getMode().equals(BasicLeg.Mode.car)){
			Road road=Road.allRoads.get(vehicle.getCurrentLink().getId().toString());
			road.enterRequest(vehicle);
		} else {
			Plan plan = vehicle.getOwnerPerson().getSelectedPlan(); // that's the plan the
			ArrayList<Object> actsLegs = plan.getActsLegs();
			Link nextLink = ((Act) actsLegs.get(vehicle.getLegIndex() + 1)).getLink();
			Road road=Road.allRoads.get(nextLink.getId().toString());
			vehicle.scheduleEndLegMessage(scheduler.simTime+vehicle.getCurrentLeg().getTravelTime(), road);
		}
	}
	
	public void logEvent() {
		BasicEvent event=null;
		
		if (eventType.equalsIgnoreCase(SimulationParameters.START_LEG)){
			event=new AgentDepartureEvent(this.getMessageArrivalTime(),vehicle.getOwnerPerson().getId().toString(),vehicle.getCurrentLink().getId().toString(),vehicle.getLegIndex()-1);
		}
		
		SimulationParameters.events.processEvent(event);
	}
	
}
