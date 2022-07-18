package org.matsim.contrib.drt.extension.shifts.operationFacilities;

import com.google.common.collect.ImmutableMap;
import org.matsim.api.core.v01.Id;

/**
 * @author nkuehhnel / MOIA
 */
public interface OperationFacilities {
    ImmutableMap<Id<OperationFacility>, OperationFacility> getDrtOperationFacilities();
}
