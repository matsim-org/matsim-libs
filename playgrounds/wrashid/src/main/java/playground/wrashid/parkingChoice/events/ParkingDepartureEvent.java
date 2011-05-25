package playground.wrashid.parkingChoice.events;

import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.PersonEvent;

import playground.wrashid.parkingChoice.infrastructure.Parking;

public class ParkingDepartureEvent {

	private AgentDepartureEvent agentDepartureEvent;
	private Parking parking;

	public ParkingDepartureEvent(final AgentDepartureEvent agentDepartureEvent, final Parking parking) {
		this.agentDepartureEvent=agentDepartureEvent;
		this.parking=parking;
	}
	
}
