package org.matsim.contrib.shifts.shift;

import org.matsim.api.core.v01.Id;

/**
 * @author nkuehnel, fzwick
 */
public class DrtShiftFactoryImpl implements DrtShiftFactory {
    @Override
    public DrtShift createShift(Id<DrtShift> id) {
        return new DrtShiftImpl(id);
    }

    @Override
    public ShiftBreak createBreak(double earliestStartTime, double latestEndTime, double duration) {
        return new DefautShiftBreakImpl(earliestStartTime, latestEndTime, duration);
    }
}
