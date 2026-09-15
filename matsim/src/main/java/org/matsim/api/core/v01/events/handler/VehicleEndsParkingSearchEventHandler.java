package org.matsim.api.core.v01.events.handler;

import org.matsim.api.core.v01.events.VehicleEndsParkingSearch;
import org.matsim.core.events.handler.EventHandler;

public interface VehicleEndsParkingSearchEventHandler extends EventHandler {
    public void handleEvent(VehicleEndsParkingSearch event);
}
