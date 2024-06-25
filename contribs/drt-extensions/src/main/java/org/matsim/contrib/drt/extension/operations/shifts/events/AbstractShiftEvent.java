package org.matsim.contrib.drt.extension.operations.shifts.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;

import java.util.Map;

/**
 * @author nkuehnel / MOIA
 */
public abstract class AbstractShiftEvent extends Event {

    public static final String ATTRIBUTE_MODE = "mode";
    private final Id<DrtShift> shiftId;

    public static final String ATTRIBUTE_SHIFT_ID = "shift_id";


    private final String mode;

    public AbstractShiftEvent(double time, String mode, Id<DrtShift> id) {
        super(time);
        this.mode = mode;
        shiftId = id;
    }

    public String getMode() {
        return mode;
    }

    public Id<DrtShift> getShiftId() {
        return shiftId;
    }

    @Override
    public Map<String, String> getAttributes() {
        Map<String, String> attr = super.getAttributes();
        attr.put(ATTRIBUTE_MODE, mode);
        attr.put(ATTRIBUTE_SHIFT_ID, shiftId + "");
        return attr;
    }
}
