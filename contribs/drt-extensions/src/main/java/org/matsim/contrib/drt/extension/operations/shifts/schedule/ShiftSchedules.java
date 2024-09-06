package org.matsim.contrib.drt.extension.operations.shifts.schedule;

import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Task;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author nkuehnel / MOIA
 */
public class ShiftSchedules {

	@SuppressWarnings("unchecked")
	public static Optional<ShiftChangeOverTask> getNextShiftChangeover (Schedule schedule) {
		int taskIdx = schedule.getStatus() == Schedule.ScheduleStatus.PLANNED ? 0 : schedule.getCurrentTask().getTaskIdx() + 1;
		return ((Stream<ShiftChangeOverTask>) schedule.tasks()
				.filter(t -> t instanceof ShiftChangeOverTask))
				.filter(t -> t.getTaskIdx() > taskIdx)
				.min(Comparator.comparingDouble(Task::getBeginTime));
	}
}
