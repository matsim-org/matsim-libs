package playground.wrashid.parkingChoice.events;

import org.matsim.api.core.v01.events.PersonDepartureEvent;

import playground.wrashid.parkingChoice.infrastructure.api.PParking;

public class ParkingDepartureEvent {

	public PersonDepartureEvent getAgentDepartureEvent() {
		return agentDepartureEvent;
	}

	public PParking getParking() {
		return parking;
	}

	private PersonDepartureEvent agentDepartureEvent;
	private PParking parking;

	public ParkingDepartureEvent(final PersonDepartureEvent agentDepartureEvent, final PParking parking) {
		this.agentDepartureEvent=agentDepartureEvent;
		this.parking=parking;
	}
	
}
