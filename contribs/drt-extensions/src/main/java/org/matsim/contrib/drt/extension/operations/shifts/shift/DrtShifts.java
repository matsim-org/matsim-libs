package org.matsim.contrib.drt.extension.operations.shifts.shift;

import org.matsim.api.core.v01.Id;

import java.util.Map;

/**
 * @author nkuehnel, fzwick / MOIA
 */
public interface DrtShifts {
    Map<Id<DrtShift>, ? extends DrtShift> getShifts();
}
