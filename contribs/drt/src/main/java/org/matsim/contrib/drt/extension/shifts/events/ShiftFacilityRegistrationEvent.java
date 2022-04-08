package org.matsim.contrib.drt.extension.shifts.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.drt.extension.shifts.operationFacilities.OperationFacility;

import java.util.Map;

/**
 * @author nkuehnel / MOIA
 */
public class ShiftFacilityRegistrationEvent extends Event {

    private final Id<DvrpVehicle> vehicleId;
    private final Id<OperationFacility> facilityId;

    public static final String ATTRIBUTE_FACILITY = "facility";
    public static final String ATTRIBUTE_VEHICLE_ID = "vehicle";

    public static final String EVENT_TYPE = "Vehicle registered at shift facility";

    public ShiftFacilityRegistrationEvent(double time, Id<DvrpVehicle> vehicleId, Id<OperationFacility> facilityId) {
        super(time);
        this.facilityId = facilityId;
        this.vehicleId = vehicleId;
    }

    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }

    @Override
    public Map<String, String> getAttributes() {
        Map<String, String> attr = super.getAttributes();
        attr.put(ATTRIBUTE_VEHICLE_ID, vehicleId + "");
        attr.put(ATTRIBUTE_FACILITY, facilityId + "");
        return attr;
    }
}

