package org.matsim.contrib.drt.extension.operations.shifts.dispatcher;

import org.apache.commons.lang.math.IntRange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilityFinder;
import org.matsim.contrib.drt.extension.operations.shifts.events.DrtShiftBreakScheduledEvent;
import org.matsim.contrib.drt.extension.operations.shifts.events.OperationFacilityRegistrationEvent;
import org.matsim.contrib.drt.extension.operations.shifts.fleet.ShiftDvrpVehicle;
import org.matsim.contrib.drt.extension.operations.shifts.scheduler.ShiftTaskScheduler;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftBreak;
import org.matsim.contrib.drt.schedule.DrtDriveTask;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;

import java.util.Set;

/**
 * @author nkuehnel / MOIA
 */
public class ShiftBreakManager {

    private final static Logger logger = LogManager.getLogger(ShiftBreakManager.class);

    private final OperationFacilityFinder facilityFinder;
    private final ShiftTaskScheduler taskScheduler;
    private final EventsManager eventsManager;
    private final MobsimTimer timer;
    private final String mode;

    public ShiftBreakManager(OperationFacilityFinder facilityFinder,
                        ShiftTaskScheduler taskScheduler,
                        EventsManager eventsManager,
                        MobsimTimer timer,
                        String mode) {
        this.facilityFinder = facilityFinder;
        this.taskScheduler = taskScheduler;
        this.eventsManager = eventsManager;
        this.timer = timer;
        this.mode = mode;
    }

    /**
     * Checks active shifts and schedules breaks when needed.
     */
    public void checkAndScheduleBreaks(Set<DrtShiftDispatcher.ShiftEntry> activeShifts) {
        for (DrtShiftDispatcher.ShiftEntry entry : activeShifts) {
            DrtShift shift = entry.shift();
            double now = timer.getTimeOfDay();
            if (!shift.isStarted() || shift.getBreak().isEmpty() ) {
                continue;
            }

            DrtShiftBreak drtShiftBreak = shift.getBreak().get();
            if(drtShiftBreak.isScheduled() ||
                    drtShiftBreak.getEarliestBreakStartTime() != now) {
                continue;
            }

            // Determine vehicle's last known link coordinate
            Coord coord = currentTaskLink(entry);

            IntRange timeRange = new IntRange(drtShiftBreak.getEarliestBreakStartTime(), drtShiftBreak.getLatestBreakEndTime());
            facilityFinder.findFacility(coord, timeRange)
                    .filter(opFa -> opFa.hasCapacity(timeRange))
                    .filter(fac -> fac.registerOrUpdateShiftBreak(entry.vehicle().getId(), timeRange))
                    .ifPresentOrElse(fac -> scheduleBreak(entry, fac),
                            () -> {
                                logger.warn("Could not schedule break for shift {} at time {}", shift.getId(), now);
                            }
                    );
        }
    }

    private Coord currentTaskLink(DrtShiftDispatcher.ShiftEntry entry) {
        Task task = entry.vehicle().getSchedule().getCurrentTask();
        if (task instanceof DrtDriveTask) {
            LinkTimePair lp = ((OnlineDriveTaskTracker) task
                    .getTaskTracker()).getDiversionPoint();
            return (lp != null ? lp.link : ((DrtDriveTask)task).getPath().getToLink()).getCoord();
        }
        if (task instanceof DrtStayTask) {
            return ((DrtStayTask)task).getLink().getCoord();
        }

        // fallback
        return ((DrtStayTask)entry.vehicle().getSchedule()
                .getTasks().get(entry.vehicle().getSchedule().getTaskCount()-1))
                .getLink().getCoord();
    }

    private void scheduleBreak(DrtShiftDispatcher.ShiftEntry entry, OperationFacility facility) {
        DrtShift shift = entry.shift();
        ShiftDvrpVehicle vehicle = entry.vehicle();
        double now = timer.getTimeOfDay();

        eventsManager.processEvent(new OperationFacilityRegistrationEvent(
                now, mode, vehicle.getId(), facility.getId()));
        taskScheduler.relocateForBreak(vehicle, facility, shift);
        eventsManager.processEvent(new DrtShiftBreakScheduledEvent(
                now, mode, shift.getId(), vehicle.getId(), facility.getLinkId(),
                shift.getBreak().get().getScheduledLatestArrival(), shift.getShiftType().orElse(null)));
    }
}
