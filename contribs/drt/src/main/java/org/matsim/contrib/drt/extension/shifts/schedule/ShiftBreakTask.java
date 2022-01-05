package org.matsim.contrib.drt.extension.shifts.schedule;

import org.matsim.contrib.drt.extension.shifts.shift.DrtShiftBreak;
import org.matsim.contrib.drt.schedule.DrtStopTask;

/**
 * @author nkuehnel / MOIA
 */
public interface ShiftBreakTask extends DrtStopTask, OperationalStop {

    DrtShiftBreak getShiftBreak();

}
