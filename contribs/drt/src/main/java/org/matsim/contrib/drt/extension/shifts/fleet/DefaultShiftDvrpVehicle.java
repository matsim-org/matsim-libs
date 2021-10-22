package org.matsim.contrib.drt.extension.shifts.fleet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.drt.extension.shifts.shift.DrtShift;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * @author nkuehnel, fzwick / MOIA
 */
public class DefaultShiftDvrpVehicle implements ShiftDvrpVehicle {

	private final DvrpVehicle vehicle;

	private final Queue<DrtShift> shifts;

	public DefaultShiftDvrpVehicle(DvrpVehicle vehicle) {
		this.vehicle = vehicle;
		this.shifts = new PriorityQueue<>(Comparator.comparingDouble(DrtShift::getStartTime));
	}

	@Override
	public Id<DvrpVehicle> getId() {
		return vehicle.getId();
	}

	@Override
	public Link getStartLink() {
		return vehicle.getStartLink();
	}

	@Override
	public int getCapacity() {
		return vehicle.getCapacity();
	}

	@Override
	public double getServiceBeginTime() {
		return vehicle.getServiceBeginTime();
	}

	@Override
	public double getServiceEndTime() {
		return vehicle.getServiceEndTime();
	}

	@Override
	public Schedule getSchedule() {
		return vehicle.getSchedule();
	}

	@Override
	public Queue<DrtShift> getShifts() {
		return shifts;
	}

	@Override
	public void addShift(DrtShift shift) {
		shifts.add(shift);
	}
}
