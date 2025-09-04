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
        if (isWithinShift(vehicle)) {
            return super.isIdle(vehicle);
        }
        return false;
    }

    public boolean isIdle(DvrpVehicle vehicle, IdleCriteria criteria) {
        if (isWithinShift(vehicle)) {
            return super.isIdle(vehicle, criteria);
        }
        return false;
    }

    private static boolean isWithinShift(DvrpVehicle vehicle) {
        if(vehicle.getSchedule().getStatus() != Schedule.ScheduleStatus.STARTED) {
            return false;
        }
        Task currentTask = vehicle.getSchedule().getCurrentTask();
        if (currentTask instanceof WaitForShiftTask) {
            return false;
        }
        if (((ShiftDvrpVehicle) vehicle).getShifts().isEmpty()) {
            return false;
        } else {
            final DrtShift peek = ((ShiftDvrpVehicle) vehicle).getShifts().peek();
            if (!peek.isStarted() || peek.isEnded()) {
                return false;
            } else {
                return true;
            }
        }
    }
}
