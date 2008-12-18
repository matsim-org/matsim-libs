package playground.wrashid.DES;

import org.matsim.events.BasicEvent;
import org.matsim.events.LinkLeaveEvent;

public class LeaveRoadMessage extends EventMessage {

	@Override
	public void handleMessage() {
		Road road = (Road) this.getReceivingUnit();
		road.leaveRoad(vehicle,getMessageArrivalTime());
	}

	public LeaveRoadMessage(Scheduler scheduler, Vehicle vehicle) {
		super(scheduler, vehicle);
		priority = SimulationParameters.PRIORITY_LEAVE_ROAD_MESSAGE;
	}

	@Override
	public void processEvent() {
		Road road = (Road) this.getReceivingUnit();
		BasicEvent event = null;

		event = new LinkLeaveEvent(this.getMessageArrivalTime(), vehicle
				.getOwnerPerson().getId().toString(), road.getLink().getId()
				.toString(), vehicle.getLegIndex() - 1);

		SimulationParameters.processEventThread.processEvent(event);
	}

}
