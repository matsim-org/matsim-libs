package org.matsim.contrib.drt.extension.operations.eshifts.fleet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.load.DvrpLoad;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.ev.fleet.ElectricFleet;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.evrp.EvDvrpVehicle;
import org.matsim.contrib.drt.extension.operations.shifts.fleet.DefaultShiftDvrpVehicle;
import org.matsim.contrib.drt.extension.operations.shifts.fleet.ShiftDvrpVehicle;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.vehicles.Vehicle;

import java.util.Queue;

/**
 * @author nkuehnel / MOIA
 */
public class EvShiftDvrpVehicle extends EvDvrpVehicle implements ShiftDvrpVehicle {

	static EvShiftDvrpVehicle create(DvrpVehicle vehicle, ElectricFleet evFleet) {
		return new EvShiftDvrpVehicle(new DefaultShiftDvrpVehicle(vehicle),
				evFleet.getElectricVehicles().get(Id.create(vehicle.getId(), Vehicle.class)));
	}

	private final ShiftDvrpVehicle vehicle;

	public EvShiftDvrpVehicle(ShiftDvrpVehicle vehicle, ElectricVehicle electricVehicle) {
		super(vehicle, electricVehicle);
		this.vehicle = vehicle;
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
		return vehicle.getShifts();
	}

	@Override
	public void addShift(DrtShift shift) {
		vehicle.addShift(shift);
	}
}
