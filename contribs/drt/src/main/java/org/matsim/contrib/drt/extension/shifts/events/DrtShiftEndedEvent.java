package org.matsim.contrib.drt.extension.shifts.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.drt.extension.shifts.shift.DrtShift;

import java.util.Map;

/**
 * @author nkuehnel / MOIA
 */
public class DrtShiftEndedEvent extends Event {

    private final Id<DrtShift> shiftId;
    private final Id<DvrpVehicle> vehicleId;
    private final Id<Link> linkId;

    public static final String ATTRIBUTE_LINK = "link";
    public static final String ATTRIBUTE_SHIFT_ID = "id";
    public static final String ATTRIBUTE_VEHICLE_ID = "vehicle";

    public static final String EVENT_TYPE = "DRT shift ended";

    public DrtShiftEndedEvent(double time, Id<DrtShift> shiftId, Id<DvrpVehicle> vehicleId, Id<Link> linkId) {
        super(time);
        this.shiftId = shiftId;
        this.vehicleId = vehicleId;
        this.linkId = linkId;
    }

    public Id<DrtShift> getShiftId() {
        return shiftId;
    }

    public Id<DvrpVehicle> getVehicleId() {
        return vehicleId;
    }

    public Id<Link> getLinkId() {
        return linkId;
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
        attr.put(ATTRIBUTE_LINK, linkId + "");
        return attr;
    }
}
