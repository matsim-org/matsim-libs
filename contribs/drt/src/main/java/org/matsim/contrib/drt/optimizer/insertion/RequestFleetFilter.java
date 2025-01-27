package org.matsim.contrib.drt.optimizer.insertion;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

import java.util.Collection;
import java.util.Map;

/**
 * @author nkuehnel / MOIA
 */
public interface RequestFleetFilter {

    default Collection<VehicleEntry> filter(DrtRequest req, Map<Id<DvrpVehicle>, VehicleEntry> vehicleEntries, double now) {
        return vehicleEntries.values();
    }

    RequestFleetFilter none = new RequestFleetFilter() {};

}
