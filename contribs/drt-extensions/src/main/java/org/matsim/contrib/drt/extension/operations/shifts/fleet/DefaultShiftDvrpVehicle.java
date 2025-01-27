package org.matsim.contrib.drt.extension.operations.shifts.fleet;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.load.DvrpLoad;
import org.matsim.contrib.dvrp.schedule.Schedule;

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
	public DvrpLoad getCapacity() {
		return vehicle.getCapacity();
	}

	@Override
	public void setCapacity(DvrpLoad capacity) {
		this.vehicle.setCapacity(capacity);
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
	public DvrpVehicleSpecification getSpecification() {
		return vehicle.getSpecification();
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
