package org.matsim.contrib.drt.extension.operations.operationFacilities;

import org.matsim.api.core.v01.Coord;

/**
 * @author nkuehnel / MOIA
 */
public interface OperationFacilityFinder {

    OperationFacility findFacilityOfType(Coord coord, OperationFacilityType type);

    OperationFacility findFacility(Coord coord);
}
