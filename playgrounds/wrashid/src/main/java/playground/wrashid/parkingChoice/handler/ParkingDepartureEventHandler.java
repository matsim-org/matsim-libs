package playground.wrashid.parkingChoice.handler;

import org.matsim.core.events.handler.EventHandler;

import playground.wrashid.parkingChoice.events.ParkingDepartureEvent;

public interface ParkingDepartureEventHandler extends EventHandler {
	public void handleEvent (ParkingDepartureEvent event);

}
