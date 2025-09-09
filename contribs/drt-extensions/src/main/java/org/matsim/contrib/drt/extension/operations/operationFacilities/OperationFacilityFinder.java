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
     * @param start Time before the facility has to be reached and be available
     * @param end Time until the facility has to be available
     */
    Optional<FacilityWithPath> findFacilityForTime(Link fromLink, DvrpVehicle dvrpVehicle, double start, double end, Set<OperationFacilityType> types);

    /**
     * @param latestArrival latest time before the facility has to be reached and be available
     * @param duration duration of required availability of the facility
     */
    Optional<FacilityWithPath> findFacilityForDuration(Link fromLink, DvrpVehicle dvrpVehicle, double latestArrival, double duration, Set<OperationFacilityType> types);
}
