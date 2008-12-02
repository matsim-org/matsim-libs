package playground.wrashid.DES;

import java.util.ArrayList;

import org.matsim.basic.v01.BasicLeg;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.BasicEvent;
import org.matsim.network.Link;
import org.matsim.population.Act;
import org.matsim.population.Plan;

public class StartingLegMessage extends EventMessage {

	public StartingLegMessage(Scheduler scheduler, Vehicle vehicle) {
		super(scheduler, vehicle);
		priority = SimulationParameters.PRIORITY_DEPARTUARE_MESSAGE;
	}

	@Override
	public void handleMessage() {

		// attempt to enter street.
		
		//if (vehicle.getOwnerPerson().getId().toString().equalsIgnoreCase("225055")){
		//	System.out.println();
		//}

		if (vehicle.getCurrentLeg().getMode().equals(BasicLeg.Mode.car)) {
			Road road = Road.getRoad(vehicle.getCurrentLink().getId()
					.toString());
			road.enterRequest(vehicle);
		} else {
			Plan plan = vehicle.getOwnerPerson().getSelectedPlan(); // that's
																	// the plan
																	// the
			ArrayList<Object> actsLegs = plan.getActsLegs();
			Link nextLink = ((Act) actsLegs.get(vehicle.getLegIndex() + 1))
					.getLink();
			Road road = Road.getRoad(nextLink.getId().toString());
			vehicle.setCurrentLink(nextLink);
			vehicle.scheduleEndLegMessage(scheduler.getSimTime()
					+ vehicle.getCurrentLeg().getTravelTime(), road);
		}
	}

	public void processEvent() {
		BasicEvent event = null;

		event = new AgentDepartureEvent(this.getMessageArrivalTime(), vehicle
				.getOwnerPerson().getId().toString(), vehicle.getCurrentLink()
				.getId().toString(), vehicle.getLegIndex() - 1);

		SimulationParameters.events.processEvent(event);
	}

}
