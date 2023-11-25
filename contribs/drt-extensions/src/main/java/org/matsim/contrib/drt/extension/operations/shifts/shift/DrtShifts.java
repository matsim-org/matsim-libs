package org.matsim.contrib.drt.extension.operations.shifts.shift;

import java.util.Map;
import org.matsim.api.core.v01.Id;

/**
 * @author nkuehnel, fzwick / MOIA
 */
public interface DrtShifts {
  Map<Id<DrtShift>, ? extends DrtShift> getShifts();
}
