package org.matsim.contrib.shifts.fleet;

import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.shifts.shift.DrtShift;

import java.util.Queue;

/**
 * @author nkuehnel, fzwick
 */
public interface ShiftDvrpVehicle extends DvrpVehicle {

	Queue<DrtShift> getShifts();

	void addShift(DrtShift shift);

}
