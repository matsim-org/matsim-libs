package org.matsim.contrib.drt.extension.operations.shifts.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;

import java.util.Map;
import java.util.Optional;

/**
 * @author nkuehnel / MOIA
 */
public abstract class AbstractShiftEvent extends Event {

    public static final String ATTRIBUTE_MODE = "mode";
    public static final String ATTRIBUTE_SHIFT_ID = "shift_id";
    public static final String ATTRIBUTE_SHIFT_TYPE = "shift_type";

    private final Id<DrtShift> shiftId;
    private final String shiftType;
    private final String mode;

    public AbstractShiftEvent(double time, String mode, Id<DrtShift> id, String shiftType) {
        super(time);
        this.mode = mode;
        this.shiftId = id;
        this.shiftType = shiftType;
    }

    public String getMode() {
        return mode;
    }

    public Id<DrtShift> getShiftId() {
        return shiftId;
    }

    public Optional<String> getShiftType() {
        return Optional.ofNullable(shiftType);
    }

    @Override
    public Map<String, String> getAttributes() {
        Map<String, String> attr = super.getAttributes();
        attr.put(ATTRIBUTE_MODE, mode);
        attr.put(ATTRIBUTE_SHIFT_ID, shiftId + "");
        if(shiftType != null) {
            attr.put(ATTRIBUTE_SHIFT_TYPE, shiftType);
        }
        return attr;
    }
}
