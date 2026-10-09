package org.matsim.contrib.drt.extension.operations.shifts.schedule;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.extension.operations.remoteoperations.schedule.IncidentHoldTask;
import org.matsim.contrib.drt.extension.operations.shifts.config.ShiftsParams;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftBreak;
import org.matsim.contrib.drt.schedule.DrtStayTaskEndTimeCalculator;
import org.matsim.contrib.drt.schedule.DrtTaskBaseType;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.ScheduleTimingUpdater;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.schedule.Task;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author nkuehnel / MOIA
 */
public class ShiftDrtStayTaskEndTimeCalculator implements ScheduleTimingUpdater.StayTaskEndTimeCalculator {

    public final static Logger logger = LogManager.getLogger(ShiftDrtStayTaskEndTimeCalculator.class);

    private final ShiftsParams drtShiftParams;
    private final DrtStayTaskEndTimeCalculator delegate;

    // Vehicles for which the "collapsing wait to zero" WARN was already logged this QSim. A vehicle whose incident hold
    // cannot be cleared (operator pool saturated, rho >= 1) stays stuck and re-triggers the collapse every sim step; the
    // vehicle staying stuck is the correct outcome (no operator can free it), so we do NOT change the behaviour - we only
    // log the WARN once per vehicle to avoid tens of thousands of identical lines. Instance-scoped: this calculator is a
    // per-QSim eager singleton, so the set naturally resets each iteration.
    private final Set<Id<DvrpVehicle>> loggedCollapse = new HashSet<>();

    public ShiftDrtStayTaskEndTimeCalculator(ShiftsParams drtShiftParams, DrtStayTaskEndTimeCalculator delegate) {
        this.drtShiftParams = drtShiftParams;
        this.delegate = delegate;
    }

    @Override
    public double calcNewEndTime(DvrpVehicle vehicle, StayTask task, double newBeginTime) {
        if (task instanceof WaitForShiftTask) {
            if (newBeginTime > task.getEndTime() && loggedCollapse.add(vehicle.getId())) {
                // A preceding delay (in remote guidance: a queued incident hold on the return-to-hub leg) pushed this
                // stay's begin past the WaitForShiftTask's fixed end. That end is the vehicle's service end for a
                // post-changeover terminal wait, or the next shift's start for a pre-shift wait; either way begin>end
                // means the vehicle reaches the wait only after it should already have ended, so the wait collapses to
                // zero duration. DVRP does not enforce serviceEndTime on a STARTED schedule, so we let it collapse
                // rather than crash (the vehicle finishes in overtime). In a plain driver-shift sim this should not
                // occur; warn so a genuine scheduling bug stays visible instead of silently degrading. A vehicle whose
                // incident hold can never be cleared (rho >= 1) stays stuck and re-triggers this every sim step - that
                // stuck outcome is correct (no operator can free it), so we keep the behaviour and only log once per
                // vehicle (guarded by the set add) to avoid flooding the log.
                logger.warn("WaitForShift begin {} pushed past its fixed end {} for vehicle {}; collapsing wait to zero"
                        + " (further occurrences for this vehicle suppressed).",
                        newBeginTime, task.getEndTime(), vehicle.getId());
            }
            return Math.max(newBeginTime, task.getEndTime());
        }
        if (task instanceof IncidentHoldTask) {
            // The hold's end is owned by the IncidentDispatcher (it is τ once an operator is serving, and is pushed
            // forward step-by-step while the incident waits in the operator queue). Preserve it verbatim — the STOP
            // delegate below would otherwise collapse the hold to zero duration (it has no passengers to board), and
            // anchoring to anything else would fight the dispatcher's dynamic end. Cf. WaitForShiftTask above.
            return Math.max(newBeginTime, task.getEndTime());
        }
        if (task instanceof ShiftBreakTask) {
            final DrtShiftBreak shiftBreak = ((ShiftBreakTask) task).getShiftBreak();
            return newBeginTime + shiftBreak.getDuration();
        } else if (task instanceof ShiftChangeOverTask) {
            // The changeover's begin time was already anchored correctly when the task was (re)built: for a regular
            // end-of-shift changeover the vehicle is held idle at the hub until the scheduled shift end (the preceding
            // stay is stretched to shift.getEndTime() by the STAY branch below), so newBeginTime >= shift.getEndTime();
            // for an actively-terminated shift (e.g. a remote-guidance recall) the begin is pulled forward, before the
            // shift's (horizon) end. In both cases the changeover must simply last changeoverDuration from its own
            // begin — anchoring to shift.getEndTime() would wrongly stretch a pulled-forward changeover back to the
            // horizon and delay the trailing WaitForShiftTask past its end.
            return newBeginTime + drtShiftParams.getChangeoverDuration();
        } else if (DrtTaskBaseType.getBaseTypeOrElseThrow(task).equals(DrtTaskBaseType.STAY)) {
            final List<? extends Task> tasks = vehicle.getSchedule().getTasks();
            final int taskIdx = tasks.indexOf(task);
            if (tasks.size() > taskIdx + 1) {
                final Task nextTask = tasks.get(taskIdx + 1);
                if (nextTask instanceof ShiftChangeOverTask) {
                    return Math.max(newBeginTime, ((ShiftChangeOverTask) nextTask).getShift().getEndTime());
                }
            }
        }
        return delegate.calcNewEndTime(vehicle, task, newBeginTime);
    }
}
