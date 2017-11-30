package playground.matsim_decoupling;

import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Task;

public class ScheduleHelper {
	public static boolean isLastTask(Schedule schedule, Task task) {
		return task.getTaskIdx() == schedule.getTaskCount() - 1;
	}

	public static boolean isNextToLastTask(Schedule schedule, Task task) {
		return task.getTaskIdx() == schedule.getTaskCount() - 2;
	}
}
