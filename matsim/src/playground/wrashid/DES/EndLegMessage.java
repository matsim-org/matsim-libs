package playground.wrashid.DES;

import java.util.ArrayList;

import org.matsim.events.BasicEvent;
import org.matsim.events.AgentArrivalEvent;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Plan;

public class EndLegMessage extends EventMessage {

	public EndLegMessage(Scheduler scheduler, Vehicle vehicle) {
		super(scheduler, vehicle);
		priority = SimulationParameters.PRIORITY_ARRIVAL_MESSAGE;
	}

	@Override
	public void handleMessage() {
		// start next leg
		// assumption: actions and legs are alternating in plans file
		vehicle.setLegIndex(vehicle.getLegIndex() + 2);
		// reset link index
		vehicle.setLinkIndex(-1);

		Plan plan = vehicle.getOwnerPerson().getSelectedPlan();
		ArrayList<Object> actsLegs = plan.getActsLegs();
		if ((actsLegs.size() > vehicle.getLegIndex())) {
			vehicle.setCurrentLeg((Leg) actsLegs.get(vehicle.getLegIndex()));
			// the leg the agent performs
			double departureTime = vehicle.getCurrentLeg().getDepartureTime();

			// if the departureTime for the leg is in the past (this means we
			// arrived late),
			// then set the departure time to the current simulation time
			// this avoids that messages in the past are put into the scheduler
			// (which makes no sense anyway)
			if (departureTime < scheduler.getSimTime()) {
				departureTime = scheduler.getSimTime();
			}

			// update current link (we arrived at a new activity)
			vehicle.setCurrentLink(((Act) actsLegs
					.get(vehicle.getLegIndex() - 1)).getLink());

			Road road = Road.getRoad(vehicle.getCurrentLink().getId()
					.toString());
			// schedule a departure from the current link in future
			vehicle.scheduleStartingLegMessage(departureTime, road);
		}

	}

	public void processEvent() {
		BasicEvent event = null;

		event = new AgentArrivalEvent(this.getMessageArrivalTime(), vehicle
				.getOwnerPerson().getId().toString(), vehicle.getCurrentLink()
				.getId().toString(), vehicle.getLegIndex() - 1);

		SimulationParameters.events.processEvent(event);
	}

}
