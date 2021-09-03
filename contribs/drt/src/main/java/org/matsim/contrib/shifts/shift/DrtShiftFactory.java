package org.matsim.contrib.shifts.shift;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.internal.MatsimFactory;

/**
 * @author nkuehnel, fzwick
 */
public interface DrtShiftFactory extends MatsimFactory {

    DrtShift createShift(Id<DrtShift> id);

    ShiftBreak createBreak(double earliestStartTime, double latestEndTime, double duration);
}
