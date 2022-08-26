package org.matsim.contrib.drt.extension.shifts.schedule;

import org.apache.log4j.Logger;
import org.matsim.contrib.drt.schedule.DrtStayTaskEndTimeCalculator;
import org.matsim.contrib.drt.schedule.DrtTaskBaseType;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.ScheduleTimingUpdater;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.drt.extension.shifts.config.DrtShiftParams;
import org.matsim.contrib.drt.extension.shifts.shift.DrtShiftBreak;

import java.util.List;

/**
 * @author nkuehnel / MOIA
 */
public class ShiftDrtStayTaskEndTimeCalculator implements ScheduleTimingUpdater.StayTaskEndTimeCalculator {

    public final static Logger logger = Logger.getLogger(ShiftDrtStayTaskEndTimeCalculator.class);

    private final DrtShiftParams drtShiftParams;
    private final DrtStayTaskEndTimeCalculator delegate;

    public ShiftDrtStayTaskEndTimeCalculator(DrtShiftParams drtShiftParams, DrtStayTaskEndTimeCalculator delegate) {
        this.drtShiftParams = drtShiftParams;
        this.delegate = delegate;
    }

    @Override
    public double calcNewEndTime(DvrpVehicle vehicle, StayTask task, double newBeginTime) {
        if(task instanceof ShiftBreakTask) {
            final DrtShiftBreak shiftBreak = ((ShiftBreakTask) task).getShiftBreak();
            return newBeginTime + shiftBreak.getDuration();
        } else if(task instanceof ShiftChangeOverTask) {
            return Math.max(newBeginTime, ((ShiftChangeOverTask) task).getShift().getEndTime()) + drtShiftParams.getChangeoverDuration();
        } else if(DrtTaskBaseType.getBaseTypeOrElseThrow(task).equals(DrtTaskBaseType.STAY)) {
            final List<? extends Task> tasks = vehicle.getSchedule().getTasks();
            final int taskIdx = tasks.indexOf(task);
            if(tasks.size() > taskIdx+1) {
                final Task nextTask = tasks.get(taskIdx +1);
                if(nextTask instanceof ShiftChangeOverTask) {
                    return Math.max(newBeginTime, ((ShiftChangeOverTask) nextTask).getShift().getEndTime());
                }
            }
        }
        return delegate.calcNewEndTime(vehicle, task, newBeginTime);
    }
}
