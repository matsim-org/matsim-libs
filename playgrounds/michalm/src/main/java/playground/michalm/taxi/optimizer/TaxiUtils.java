package playground.michalm.taxi.optimizer;

import java.util.Iterator;

import pl.poznan.put.vrp.dynamic.data.model.Vehicle;
import pl.poznan.put.vrp.dynamic.data.schedule.*;
import pl.poznan.put.vrp.dynamic.data.schedule.Schedule.ScheduleStatus;
import pl.poznan.put.vrp.dynamic.data.schedule.Task.TaskType;
import playground.michalm.taxi.optimizer.schedule.TaxiDriveTask;


public class TaxiUtils
{
    public static boolean isIdle(Vehicle vehicle, int time, boolean delayedWaitTaskAsNonIdle)
    {
        Schedule sched = vehicle.getSchedule();

        if (sched.getStatus() != ScheduleStatus.STARTED) {
            return false;
        }

        Task currentTask = sched.getCurrentTask();

        switch (currentTask.getType()) {
            case SERVE:
                return false;

            case DRIVE:
                TaxiDriveTask tdt = (TaxiDriveTask)currentTask;

                switch (tdt.getDriveType()) {
                    case PICKUP:
                    case DELIVERY:
                        return false;

                    case CRUISE:
                        // TODO this requires some analysis if a vehicle en route can be immediately
                        // diverted or there is a lag (as in the case of WAIT);
                        // how long is the lag??
                        System.err.println("Currently CRUISE cannot be interrupted, so the vehicle is considered BUSY...");
                        return false;

                    default:
                        throw new IllegalStateException();
                }

            case WAIT:
                if (delayedWaitTaskAsNonIdle && isCurrentTaskDelayed(sched, time)) {
                    return false;// assuming that the next task is a non-wait task
                }

                break;

            default:
                throw new IllegalStateException();
        }

        // idle right now, but:
        // consider CLOSING (T1) time windows of the vehicle
        if (time >= Schedules.getActualT1(sched)) {
            return false;
        }

        return true;
    }


    public static boolean isCurrentTaskDelayed(Schedule schedule, int time)
    {
        Task currentTask = schedule.getCurrentTask();
        int delay = time - currentTask.getEndTime();

        if (delay < 0) {
            return false;
        }

        //TODO vrp library should be "MATSim free"; here, however, some MATSim-related hacks
        //have been introduced!!!
        if (currentTask.getType() == TaskType.WAIT && delay >= 2) {
            // there can be a lag between a change in the schedule (WAIT->OTHER)
            // because activity ends (here, WAIT end) are handled only at the beginning of
            // a simulation step, i.e. ActivityEngine is before QNetsimEngine
            // According to some code analysis, the lag should not be larger than 1 second
            // TODO BTW. Is "ActivityEngine before QNetsimEngine" the only approach???
            System.err.println("TaxiUtils.isCurrentTaskDelayed(Schedule schedule, int time): "
                    + "This is very unlikely! I am just curious if this ever happens:-)");
        }

        return true;
    }


    // used for debugging
    @SuppressWarnings("unused")
    private static void printScheduledRequests(Vehicle veh)
    {
        Schedule sched = veh.getSchedule();
        ScheduleStatus status = sched.getStatus();

        String currentTaskId;

        switch (status) {
            case UNPLANNED:
                currentTaskId = "-/";
                break;

            case PLANNED:
                currentTaskId = "0/";
                break;

            case STARTED:
                currentTaskId = (sched.getCurrentTask().getTaskIdx() + 1) + "/";
                break;

            case COMPLETED:
                currentTaskId = "-/";
                break;

            default:
                throw new IllegalStateException("Unsupported state");
        }

        StringBuilder schedLine = new StringBuilder("Veh: " + veh.getName() + " : " + status + " ["
                + currentTaskId + sched.getTaskCount() + "]");
        Iterator<ServeTask> stIter = Schedules.createServeTaskIter(sched);

        while (stIter.hasNext()) {
            ServeTask st = stIter.next();
            schedLine.append("-").append(st.getRequest());
        }

        System.err.println(schedLine.toString());
    }
}
