package playground.clruch.dispatcher.utils;

import playground.clruch.dispatcher.core.VehicleLinkPair;

public class ReserveVehiclesStoppingOnLink extends AbstractVehiclesOnLink {

    @Override
    public boolean shouldStop(VehicleLinkPair vehicleLinkPair) {
        return vehicleLinkPair.isDivertableLocationAlsoDriveTaskDestination();
    }
}
