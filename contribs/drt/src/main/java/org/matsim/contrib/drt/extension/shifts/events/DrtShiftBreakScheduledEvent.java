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
public class DrtShiftBreakScheduledEvent extends Event {

    private final Id<DrtShift> shiftId;
    private final Id<DvrpVehicle> vehicleId;
    private final Id<Link> linkId;
    private final double latestArrival;

    public static final String ATTRIBUTE_SHIFT_ID = "shiftId";
    public static final String ATTRIBUTE_VEHICLE_ID = "vehicle";
    public static final String ATTRIBUTE_LINK_ID = "link";
    public static final String ATTRIBUTE_LATEST_ARRIVAL = "latestArrival";

    public static final String EVENT_TYPE = "DRT shift break scheduled";

    public DrtShiftBreakScheduledEvent(double timeOfDay, Id<DrtShift> id, Id<DvrpVehicle> id1, Id<Link> linkId, double latestArrival) {
        super(timeOfDay);
        shiftId = id;
        vehicleId = id1;
        this.linkId = linkId;
        this.latestArrival = latestArrival;
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
        attr.put(ATTRIBUTE_LINK_ID, linkId + "");
        attr.put(ATTRIBUTE_LATEST_ARRIVAL, latestArrival + "");
        return attr;
    }
}
