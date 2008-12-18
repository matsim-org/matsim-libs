package playground.wrashid.DES;

import org.matsim.basic.v01.BasicLeg;
import org.matsim.events.ActEndEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.BasicEvent;

public class StartingLegMessage extends EventMessage {

	public StartingLegMessage(Scheduler scheduler, Vehicle vehicle) {
		super(scheduler, vehicle);
		priority = SimulationParameters.PRIORITY_DEPARTUARE_MESSAGE;
	}

	@Override
	public void handleMessage() {
		// if current leg is in car mode, then enter request in first road
		if (vehicle.getCurrentLeg().getMode().equals(BasicLeg.Mode.car)) {
			Road road = Road.getRoad(vehicle.getCurrentLink().getId()
					.toString());
			road.enterRequest(vehicle,getMessageArrivalTime());
		} else {
			// move to first link in next leg and schedule an end leg message
			vehicle.moveToFirstLinkInNextLeg();
			Road road = Road.getRoad(vehicle.getCurrentLink().getId()
					.toString());

			vehicle.scheduleEndLegMessage(getMessageArrivalTime()
					+ vehicle.getCurrentLeg().getTravelTime(), road);
		}
	}

	public void processEvent() {
		BasicEvent event = null;

		// schedule ActEndEvent
		event = new ActEndEvent(this.getMessageArrivalTime(), vehicle
				.getOwnerPerson().getId().toString(), vehicle.getCurrentLink()
				.getId().toString(), vehicle.getPreviousActivity().getType());
		SimulationParameters.events.processEvent(event);
		
		// schedule AgentDepartureEvent
		event = new AgentDepartureEvent(this.getMessageArrivalTime(), vehicle
				.getOwnerPerson().getId().toString(), vehicle.getCurrentLink()
				.getId().toString(), vehicle.getLegIndex() - 1);

		SimulationParameters.events.processEvent(event);
		
	}

}
