package org.matsim.contrib.drt.extension.alonso_mora.shifts;

import org.matsim.contrib.drt.extension.alonso_mora.scheduling.DefaultAlonsoMoraScheduler.OperationalVoter;
import org.matsim.contrib.drt.extension.shifts.schedule.OperationalStop;
import org.matsim.contrib.drt.extension.shifts.schedule.ShiftBreakTask;
import org.matsim.contrib.drt.extension.shifts.schedule.ShiftChangeOverTask;
import org.matsim.contrib.drt.extension.shifts.scheduler.ShiftTaskScheduler;
import org.matsim.contrib.drt.schedule.DrtDriveTask;
import org.matsim.contrib.drt.schedule.DrtTaskType;
import org.matsim.contrib.dvrp.schedule.Task;

public class ShiftOperationalVoter implements OperationalVoter {
	@Override
	public boolean isOperationalTask(Task task) {
		return task instanceof OperationalStop;
	}

	@Override
	public DrtTaskType getDriveTaskType(Task task) {
		if (task instanceof ShiftChangeOverTask) {
			return ShiftTaskScheduler.RELOCATE_VEHICLE_SHIFT_CHANGEOVER_TASK_TYPE;
		} else if (task instanceof ShiftBreakTask) {
			return ShiftTaskScheduler.RELOCATE_VEHICLE_SHIFT_BREAK_TASK_TYPE;
		} else {
			return DrtDriveTask.TYPE;
		}
	}
}