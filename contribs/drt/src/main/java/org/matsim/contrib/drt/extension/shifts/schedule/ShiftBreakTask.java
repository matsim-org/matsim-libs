package org.matsim.contrib.drt.extension.shifts.schedule;

import org.matsim.contrib.drt.extension.shifts.shift.DrtShiftBreak;

/**
 * @author nkuehnel / MOIA
 */
public interface ShiftBreakTask extends OperationalStop {

    DrtShiftBreak getShiftBreak();

}
