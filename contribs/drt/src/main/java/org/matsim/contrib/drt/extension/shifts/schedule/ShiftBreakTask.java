package org.matsim.contrib.drt.extension.shifts.schedule;

import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.drt.extension.shifts.shift.DrtShiftBreak;

/**
 * @author nkuehnel / MOIA
 */
public interface ShiftBreakTask extends Task, OperationalStop {

    DrtShiftBreak getShiftBreak();

}
