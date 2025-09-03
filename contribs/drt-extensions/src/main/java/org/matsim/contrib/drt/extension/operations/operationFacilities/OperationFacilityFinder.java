package org.matsim.contrib.drt.extension.operations.operationFacilities;

import org.matsim.api.core.v01.Coord;

import java.util.Optional;
import java.util.Set;

/**
 * @author nkuehnel / MOIA
 */
public interface OperationFacilityFinder {

    Optional<OperationFacility> findFacility(Coord coord, double fromInclusive, double toInclusive, Set<OperationFacilityType> types);

    Optional<OperationFacility> findFacility(Coord coord, double fromInclusive, Set<OperationFacilityType> types);
}
