package org.matsim.contrib.drt.extension.operations.shifts.schedule;

import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftBreak;
import org.matsim.contrib.drt.schedule.DrtStopTask;

/**
 * Interface representing a task for a shift break.
 * Supports dynamically adding or removing charging capabilities (via {@link FacilityStop}).
 *
 * @author nkuehnel / MOIA
 */
public interface ShiftBreakTask extends DrtStopTask, FacilityStop {

    /**
     * @return The shift break associated with this task
     */
    DrtShiftBreak getShiftBreak();

}