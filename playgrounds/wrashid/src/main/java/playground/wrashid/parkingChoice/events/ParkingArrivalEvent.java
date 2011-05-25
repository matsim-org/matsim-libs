package playground.wrashid.parkingChoice.events;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.PersonEvent;

import playground.wrashid.parkingChoice.infrastructure.Parking;

public class ParkingArrivalEvent {

	private ActivityStartEvent actStartEvent;
	private Parking parking;

	public ParkingArrivalEvent(final ActivityStartEvent actStartEvent, final Parking parking) {
		this.actStartEvent=actStartEvent;
		this.parking=parking;
	}
	
}
