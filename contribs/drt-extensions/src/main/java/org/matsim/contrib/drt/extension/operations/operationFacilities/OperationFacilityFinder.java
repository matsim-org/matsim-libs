package org.matsim.contrib.drt.extension.operations.operationFacilities;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;

import java.util.Optional;
import java.util.Set;

/**
 * @author nkuehnel / MOIA
 */
public interface OperationFacilityFinder {

    record FacilityWithPath(OperationFacility operationFacility, VrpPathWithTravelData path){}

    Optional<FacilityWithPath> findFacility(Link fromLink, DvrpVehicle dvrpVehicle, Set<OperationFacilityType> types);

    /**
     */
    Optional<FacilityWithPath> findFacilityForTime(Link fromLink,
                                                   DvrpVehicle dvrpVehicle,
                                                   double departureTime,
                                                   double latestArrival,
                                                   double reservationEndTime,
                                                   Set<OperationFacilityType> types);
}
