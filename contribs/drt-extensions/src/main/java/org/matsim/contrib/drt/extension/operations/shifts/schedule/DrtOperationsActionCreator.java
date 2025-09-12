package org.matsim.contrib.drt.extension.operations.shifts.schedule;

import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.passenger.PassengerHandler;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic.DynActionCreator;
import org.matsim.contrib.dynagent.DynAction;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.contrib.evrp.EvDvrpVehicle;
import org.matsim.contrib.evrp.tracker.OfflineETaskTracker;
import org.matsim.core.mobsim.framework.MobsimTimer;


/**
 * Unified action creator that works with both standard and electric vehicles using the unified task implementations.
 * This class replaces both ShiftDrtActionCreator and ShiftEDrtActionCreator.
 *
 * @author nkuehnel / MOIA
 */
public class DrtOperationsActionCreator implements DynActionCreator {

    private final PassengerHandler passengerHandler;
    private final DynActionCreator delegate;

    private final MobsimTimer timer;

    /**
     * Constructor for standard vehicles (no electric capability)
     */
    public DrtOperationsActionCreator(PassengerHandler passengerHandler, DynActionCreator delegate) {
        this(passengerHandler, delegate, null);
    }

    /**
     * Constructor for both standard and electric vehicles
     */
    public DrtOperationsActionCreator(PassengerHandler passengerHandler, DynActionCreator delegate, MobsimTimer timer) {
        this.passengerHandler = passengerHandler;
        this.delegate = delegate;
        this.timer = timer;
    }

    @Override
    public DynAction createAction(DynAgent dynAgent, DvrpVehicle vehicle, double now) {
        Task task = vehicle.getSchedule().getCurrentTask();

        // Handle ShiftBreakTask
        if (task instanceof ShiftBreakTask breakTask) {
            // Check if this is an electric vehicle with charging
            if (vehicle instanceof EvDvrpVehicle && breakTask.getChargingTask().isPresent() && timer != null) {
                initEvTaskTracker(task, (EvDvrpVehicle) vehicle);
            }
            return new ShiftBreakActivity(
                    passengerHandler,
                    dynAgent,
                    breakTask,
                    breakTask.getDropoffRequests(),
                    breakTask.getPickupRequests());

        }

        // Handle ShiftChangeOverTask
        if (task instanceof ShiftChangeOverTask changeoverTask) {
            // Check if this is an electric vehicle with charging
            if (vehicle instanceof EvDvrpVehicle && changeoverTask.getChargingTask().isPresent() && timer != null) {
                initEvTaskTracker(task, (EvDvrpVehicle) vehicle);
            }
            return new ChangeoverActivity(
                    passengerHandler,
                    dynAgent,
                    changeoverTask,
                    changeoverTask.getDropoffRequests(),
                    changeoverTask.getPickupRequests());

        }

        // Handle WaitForShiftTask
        if (task instanceof WaitForShiftTask waitTask) {
            // Initialize tracker for electric vehicles if needed
            if (vehicle instanceof EvDvrpVehicle && timer != null) {
                initEvTaskTracker(task, (EvDvrpVehicle) vehicle);
            }

            // Always use dynamic activity to support adding charging later
            return new WaitForShiftActivity(waitTask);
        }

        // For all other task types, delegate to the provided delegate
        DynAction action = delegate.createAction(dynAgent, vehicle, now);

        // Initialize tracker for electric vehicles if needed
        if (vehicle instanceof EvDvrpVehicle && timer != null && task.getTaskTracker() == null) {
            initEvTaskTracker(task, (EvDvrpVehicle) vehicle);
        }

        return action;
    }

    /**
     * Initialize the task tracker for an electric vehicle task
     */
    private void initEvTaskTracker(Task task, EvDvrpVehicle vehicle) {
        if (timer != null && task.getTaskTracker() == null) {
            task.initTaskTracker(new OfflineETaskTracker(vehicle, timer));
        }
    }
}