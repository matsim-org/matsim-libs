package org.matsim.contrib.shifts.operationFacilities;

import com.google.common.collect.ImmutableMap;
import org.matsim.api.core.v01.Id;

/**
 * @author nkuehhnel
 */
public interface OperationFacilities {
    ImmutableMap<Id<OperationFacility>, OperationFacility> getDrtOperationFacilities();

    void addOperationFacility(OperationFacility facility);

    OperationFacility removeOperationFacility(Id<OperationFacility> facilityId);
}
