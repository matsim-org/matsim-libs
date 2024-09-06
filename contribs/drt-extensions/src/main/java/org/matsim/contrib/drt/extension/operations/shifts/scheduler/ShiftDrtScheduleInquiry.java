package org.matsim.contrib.drt.extension.operations.shifts.scheduler;

import com.google.inject.Inject;
import org.matsim.contrib.drt.extension.operations.shifts.fleet.ShiftDvrpVehicle;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.WaitForShiftTask;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.drt.scheduler.DrtScheduleInquiry;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.ScheduleInquiry;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.core.mobsim.framework.MobsimTimer;

import static org.matsim.contrib.drt.schedule.DrtTaskBaseType.STAY;

/**
 * @author nkuehnel / MOIA
 */
public class ShiftDrtScheduleInquiry extends DrtScheduleInquiry implements ScheduleInquiry {

    @Inject
    private MobsimTimer timer;

    @Inject
    public ShiftDrtScheduleInquiry(MobsimTimer timer) {
        super(timer);
    }

    @Override
    public boolean isIdle(DvrpVehicle vehicle) {
        Schedule schedule = vehicle.getSchedule();
        if (timer.getTimeOfDay() >= vehicle.getServiceEndTime() || schedule.getStatus() != Schedule.ScheduleStatus.STARTED) {
            return false;
        }

        if(((ShiftDvrpVehicle) vehicle).getShifts().isEmpty()) {
            return false;
        } else {
            final DrtShift peek = ((ShiftDvrpVehicle) vehicle).getShifts().peek();
            if(!peek.isStarted() || peek.isEnded()) {
                return false;
            }
        }
        Task currentTask = schedule.getCurrentTask();
        if(currentTask instanceof WaitForShiftTask) {
            return false;
        }
        return currentTask.getTaskIdx() == schedule.getTaskCount() - 1
                && STAY.isBaseTypeOf(currentTask);
    }

}
