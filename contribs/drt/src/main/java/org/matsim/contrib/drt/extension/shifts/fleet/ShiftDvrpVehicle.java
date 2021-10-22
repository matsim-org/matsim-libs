package org.matsim.contrib.drt.extension.shifts.fleet;

import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.drt.extension.shifts.shift.DrtShift;

import java.util.Queue;

/**
 * @author nkuehnel, fzwick / MOIA
 */
public interface ShiftDvrpVehicle extends DvrpVehicle {

	Queue<DrtShift> getShifts();

	void addShift(DrtShift shift);

}
