package org.matsim.contrib.drt.extension.operations.shifts.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;

import java.util.Map;

/**
 * @author nkuehnel / MOIA
 */
public class VehicleLeftShiftFacilityEvent extends Event {

    private final String mode;
    private final Id<DvrpVehicle> vehicleId;
    private final Id<OperationFacility> facilityId;

    public static final String ATTRIBUTE_MODE = "mode";

    public static final String ATTRIBUTE_FACILITY = "facility";
    public static final String ATTRIBUTE_VEHICLE_ID = "vehicle";

    public static final String EVENT_TYPE = "Vehicle left shift facility";

    public VehicleLeftShiftFacilityEvent(double time, String mode, Id<DvrpVehicle> vehicleId, Id<OperationFacility> facilityId) {
        super(time);
        this.mode = mode;
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
        attr.put(ATTRIBUTE_MODE, mode);
        attr.put(ATTRIBUTE_VEHICLE_ID, vehicleId + "");
        attr.put(ATTRIBUTE_FACILITY, facilityId + "");
        return attr;
    }
}
