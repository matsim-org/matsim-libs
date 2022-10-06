package org.matsim.contrib.drt.extension.operations.shifts.fleet;

import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

import java.util.Queue;

/**
 * @author nkuehnel, fzwick / MOIA
 */
public interface ShiftDvrpVehicle extends DvrpVehicle {

	Queue<DrtShift> getShifts();

	void addShift(DrtShift shift);

}
