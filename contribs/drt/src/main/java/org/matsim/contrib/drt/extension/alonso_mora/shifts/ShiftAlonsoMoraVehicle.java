package org.matsim.contrib.drt.extension.alonso_mora.shifts;

import org.matsim.contrib.drt.extension.alonso_mora.algorithm.DefaultAlonsoMoraVehicle;
import org.matsim.contrib.drt.extension.shifts.schedule.OperationalStop;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.util.LinkTimePair;

public class ShiftAlonsoMoraVehicle extends DefaultAlonsoMoraVehicle {
	private final DvrpVehicle vehicle;

	public ShiftAlonsoMoraVehicle(DvrpVehicle vehicle) {
		super(vehicle);
		this.vehicle = vehicle;
	}

	@Override
	public LinkTimePair getNextDiversion(double now) {
		Schedule schedule = vehicle.getSchedule();

		if (schedule.getStatus().equals(ScheduleStatus.STARTED)) {
			Task task = schedule.getCurrentTask();

			if (task instanceof StayTask) {
				if (task instanceof OperationalStop) {
					// If we are in an operational stop, we can only continue after it is done!
					return new LinkTimePair(((StayTask) task).getLink(), task.getEndTime());
				}
			}
		}

		return super.getNextDiversion(now);
	}
}
