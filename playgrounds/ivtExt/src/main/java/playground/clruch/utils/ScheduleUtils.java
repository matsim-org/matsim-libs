// code by jph
package playground.clruch.utils;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Task;

import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.schedule.AVStayTask;

public class ScheduleUtils {
    public static String toString(Schedule schedule) {
        StringBuilder stringBuilder = new StringBuilder();
        boolean flag = false;
        int hiddenCount = 0;
        for (Task task : schedule.getTasks()) {
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
        Schedule schedule = avVehicle.getSchedule();
        return toString(schedule);
    }

    /**
     * @param avVehicle
     * @param taskEndTime
     *            has to be strictly less than scheduleEndTime
     * @param scheduleEndTime
     * @param destination
     */
    public static void makeWhole( //
            AVVehicle avVehicle, double taskEndTime, double scheduleEndTime, Link destination) {
        if (taskEndTime < scheduleEndTime) {
            Schedule schedule = avVehicle.getSchedule();
            schedule.addTask(new AVStayTask(taskEndTime, scheduleEndTime, destination));
        } else {
            throw new IllegalArgumentException("taskEndTime " + taskEndTime + " > scheduleEndTime " + scheduleEndTime);
        }
    }

}
