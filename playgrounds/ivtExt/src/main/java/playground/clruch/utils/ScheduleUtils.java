package playground.clruch.utils;

import org.matsim.contrib.dvrp.schedule.AbstractTask;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Task;

import playground.sebhoerl.avtaxi.data.AVVehicle;

public class ScheduleUtils {
    public static String toString(Schedule<AbstractTask> schedule) {
        StringBuilder stringBuilder = new StringBuilder();
        boolean flag = false;
        int hiddenCount = 0;
        for (AbstractTask task : schedule.getTasks()) {
            boolean isStarted = task.getStatus().equals(Task.TaskStatus.STARTED);
            if (isStarted && !flag)
                stringBuilder.append("_X( " + hiddenCount + " ... )\n");
            flag |= isStarted;
            if (flag) {
                stringBuilder.append(isStarted ? ">" : " ");
                stringBuilder.append(task.toString());
                stringBuilder.append('\n');
            } else {
                ++hiddenCount;
            }
        }
        return stringBuilder.toString().trim();
    }

    public static String scheduleOf(AVVehicle avVehicle) {
        Schedule<AbstractTask> schedule = (Schedule<AbstractTask>) avVehicle.getSchedule();
        return toString(schedule);
    }
}
