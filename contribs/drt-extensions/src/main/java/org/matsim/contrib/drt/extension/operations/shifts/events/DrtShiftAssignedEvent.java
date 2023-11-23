package org.matsim.contrib.drt.extension.operations.shifts.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

import java.util.Map;

/**
 * @author nkuehnel / MOIA
 */
public class DrtShiftAssignedEvent extends Event {

    private final Id<DrtShift> shiftId;
    private final Id<DvrpVehicle> vehicleId;

    public static final String ATTRIBUTE_SHIFT_ID = "id";
    public static final String ATTRIBUTE_VEHICLE_ID = "vehicle";

    public static final String EVENT_TYPE = "DRT shift assigned";

    public DrtShiftAssignedEvent(double timeOfDay, Id<DrtShift> id, Id<DvrpVehicle> id1) {
        super(timeOfDay);
        shiftId = id;
        vehicleId = id1;
    }

    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }

    @Override
    public Map<String, String> getAttributes() {
        Map<String, String> attr = super.getAttributes();
        attr.put(ATTRIBUTE_SHIFT_ID, shiftId + "");
        attr.put(ATTRIBUTE_VEHICLE_ID, vehicleId + "");
        return attr;
    }
}
