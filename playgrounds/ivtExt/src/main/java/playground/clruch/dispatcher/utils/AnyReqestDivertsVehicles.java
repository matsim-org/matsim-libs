package playground.clruch.dispatcher.utils;

import playground.clruch.dispatcher.core.VehicleLinkPair;

public class AnyReqestDivertsVehicles extends AbstractVehiclesOnLink {

    @Override
    public boolean shouldStop(VehicleLinkPair vehicleLinkPair) {
        return true;
    }

}
