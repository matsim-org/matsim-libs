package org.matsim.contrib.drt.extension.operations.shifts.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

import java.util.Map;

/**
 * @author nkuehnel / MOIA
 */
public class DrtShiftEndedEvent extends Event {

    private final Id<DrtShift> shiftId;
    private final Id<DvrpVehicle> vehicleId;
    private final Id<Link> linkId;
	private final Id<OperationFacility> operationFacilityId;

	public static final String ATTRIBUTE_LINK = "link";
	public static final String ATTRIBUTE_OPERATION_FACILITY = "operationFacility";
    public static final String ATTRIBUTE_SHIFT_ID = "id";
    public static final String ATTRIBUTE_VEHICLE_ID = "vehicle";

    public static final String EVENT_TYPE = "DRT shift ended";

    public DrtShiftEndedEvent(double time, Id<DrtShift> shiftId, Id<DvrpVehicle> vehicleId,
							  Id<Link> linkId, Id<OperationFacility> operationFacilityId) {
        super(time);
        this.shiftId = shiftId;
        this.vehicleId = vehicleId;
        this.linkId = linkId;
		this.operationFacilityId = operationFacilityId;
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

	public Id<OperationFacility> getOperationFacilityId() {
		return operationFacilityId;
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
        attr.put(ATTRIBUTE_OPERATION_FACILITY, operationFacilityId + "");
        return attr;
    }
}
