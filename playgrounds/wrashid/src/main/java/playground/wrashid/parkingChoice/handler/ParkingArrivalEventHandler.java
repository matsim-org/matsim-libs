package playground.wrashid.parkingChoice.handler;

import org.matsim.core.events.handler.EventHandler;

import playground.wrashid.parkingChoice.events.ParkingArrivalEvent;

public interface ParkingArrivalEventHandler extends EventHandler {
	public void handleEvent (ParkingArrivalEvent event);
}
