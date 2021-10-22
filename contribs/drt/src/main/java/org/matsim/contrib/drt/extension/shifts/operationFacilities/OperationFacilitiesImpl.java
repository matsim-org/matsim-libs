package org.matsim.contrib.drt.extension.shifts.operationFacilities;

import com.google.common.collect.ImmutableMap;
import org.matsim.api.core.v01.Id;

import java.util.HashMap;
import java.util.Map;

/**
 * @author nkuehnel
 */
public class OperationFacilitiesImpl implements OperationFacilities {

    private final Map<Id<OperationFacility>, OperationFacility> facilities = new HashMap<>();

    @Override
    public ImmutableMap<Id<OperationFacility>, OperationFacility> getDrtOperationFacilities() {
        return ImmutableMap.copyOf(facilities);
    }

    @Override
    public void addOperationFacility(OperationFacility facility) {
        facilities.put(facility.getId(), facility);
    }

    @Override
    public OperationFacility removeOperationFacility(Id<OperationFacility> facilityId) {
        return facilities.remove(facilityId);
    }
}
