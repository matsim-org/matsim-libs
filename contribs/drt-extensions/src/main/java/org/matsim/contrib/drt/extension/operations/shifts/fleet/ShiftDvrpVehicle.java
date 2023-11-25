package org.matsim.contrib.drt.extension.operations.shifts.fleet;

import java.util.Queue;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

/**
 * @author nkuehnel, fzwick / MOIA
 */
public interface ShiftDvrpVehicle extends DvrpVehicle {

  Queue<DrtShift> getShifts();

  void addShift(DrtShift shift);
}
