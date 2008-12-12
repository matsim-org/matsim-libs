package playground.wrashid.DES;

import org.matsim.events.BasicEvent;
import org.matsim.events.LinkEnterEvent;

public class EnterRoadMessage extends EventMessage {

	@Override
	public void handleMessage() {
		// enter the next road
		Road road = Road.getRoad(vehicle.getCurrentLink().getId().toString());
		road.enterRoad(vehicle,getMessageArrivalTime());
	}

	public EnterRoadMessage(Scheduler scheduler, Vehicle vehicle) {
		super(scheduler, vehicle);
		priority = SimulationParameters.PRIORITY_ENTER_ROAD_MESSAGE;
	}

	public void processEvent() {
		BasicEvent event = null;

		event = new LinkEnterEvent(this.getMessageArrivalTime(), vehicle
				.getOwnerPerson().getId().toString(), vehicle.getCurrentLink()
				.getId().toString(), vehicle.getLegIndex() - 1);

		SimulationParameters.events.processEvent(event);
	}

}
