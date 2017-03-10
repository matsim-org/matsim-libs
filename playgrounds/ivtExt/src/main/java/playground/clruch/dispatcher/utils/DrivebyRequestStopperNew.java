package playground.clruch.dispatcher.utils;

import org.matsim.api.core.v01.network.Link;
import playground.clruch.dispatcher.core.VehicleLinkPair;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by Claudio on 3/10/2017.
 */
public class DrivebyRequestStopperNew extends AbstractVehicleDestMatcher {
    @Override
    protected Map<VehicleLinkPair, Link> protected_match(Collection<VehicleLinkPair> vehicleLinkPairs, List<Link> links) {
        return null;
    }
}
