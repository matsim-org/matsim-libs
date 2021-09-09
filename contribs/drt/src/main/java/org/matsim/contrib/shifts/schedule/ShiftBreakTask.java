package org.matsim.contrib.shifts.schedule;

import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.shifts.shift.ShiftBreak;

/**
 * @author nkuehnel
 */
public interface ShiftBreakTask extends Task, OperationalStop {

    ShiftBreak getShiftBreak();

}
