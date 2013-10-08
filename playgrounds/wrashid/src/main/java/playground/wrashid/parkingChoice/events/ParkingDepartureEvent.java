package playground.wrashid.parkingChoice.events;

import org.matsim.api.core.v01.events.PersonDepartureEvent;

import playground.wrashid.parkingChoice.infrastructure.api.Parking;

public class ParkingDepartureEvent {

	public PersonDepartureEvent getAgentDepartureEvent() {
		return agentDepartureEvent;
	}

	public Parking getParking() {
		return parking;
	}

	private PersonDepartureEvent agentDepartureEvent;
	private Parking parking;

	public ParkingDepartureEvent(final PersonDepartureEvent agentDepartureEvent, final Parking parking) {
		this.agentDepartureEvent=agentDepartureEvent;
		this.parking=parking;
	}
	
}
