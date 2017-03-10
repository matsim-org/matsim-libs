package playground.clruch.dispatcher.utils;

import playground.clruch.dispatcher.core.VehicleLinkPair;

/**
 * Created by Claudio on 3/10/2017.
 * new version of DrivebyRequestStopper.java implements AbstractVehicleRequestLinkMatcher
 * 
 * See if any car is driving by a request. If so, cancel driving path and make link new goal.
 * Then stay there to be matched ASAP or become available for rerouting.
 */
public class RequestStopsDrivebyVehicles extends AbstractVehiclesOnLink {
    @Override
    public boolean shouldStop(VehicleLinkPair vehicleLinkPair) {
        return !vehicleLinkPair.isDivertableLocationAlsoDriveTaskDestination();
    }
}
