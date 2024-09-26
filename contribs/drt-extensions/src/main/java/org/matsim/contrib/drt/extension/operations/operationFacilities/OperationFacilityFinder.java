package org.matsim.contrib.drt.extension.operations.operationFacilities;

import org.matsim.api.core.v01.Coord;

import java.util.Optional;

/**
 * @author nkuehnel / MOIA
 */
public interface OperationFacilityFinder {

    Optional<OperationFacility> findFacilityOfType(Coord coord, OperationFacilityType type);

    Optional<OperationFacility> findFacility(Coord coord);
}
