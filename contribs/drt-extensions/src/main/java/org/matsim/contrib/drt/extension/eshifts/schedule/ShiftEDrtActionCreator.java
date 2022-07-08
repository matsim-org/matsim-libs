package org.matsim.contrib.drt.extension.eshifts.schedule;

import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.drt.vrpagent.DrtActionCreator;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.passenger.PassengerHandler;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTrackerImpl;
import org.matsim.contrib.dvrp.tracker.OnlineTrackerListener;
import org.matsim.contrib.dvrp.tracker.TaskTrackers;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic.DynActionCreator;
import org.matsim.contrib.dvrp.vrpagent.VrpLeg;
import org.matsim.contrib.dynagent.DynAction;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.contrib.drt.extension.eshifts.charging.ChargingBreakActivity;
import org.matsim.contrib.drt.extension.eshifts.charging.ChargingChangeoverActivity;
import org.matsim.contrib.drt.extension.eshifts.charging.ChargingWaitForShiftActivity;
import org.matsim.contrib.evrp.EvDvrpVehicle;
import org.matsim.contrib.evrp.tracker.OfflineETaskTracker;
import org.matsim.contrib.evrp.tracker.OnlineEDriveTaskTracker;
import org.matsim.contrib.drt.extension.shifts.schedule.ShiftDrtActionCreator;
import org.matsim.core.mobsim.framework.MobsimTimer;

/**
 * @author nkuehnel / MOIA
 */
public class ShiftEDrtActionCreator implements DynActionCreator {

    private final DynActionCreator drtActionCreator;
    private final MobsimTimer timer;
    private final PassengerHandler passengerHandler;

    public ShiftEDrtActionCreator(PassengerHandler passengerHandler, MobsimTimer timer, DvrpConfigGroup dvrpCfg) {
        this.timer = timer;
        this.passengerHandler = passengerHandler;
        drtActionCreator = new ShiftDrtActionCreator(passengerHandler, new DrtActionCreator(passengerHandler,
                v -> createLeg(dvrpCfg.getMobsimMode(), v, timer)));
    }

    public ShiftEDrtActionCreator(DynActionCreator delegate, MobsimTimer timer, PassengerHandler passengerHandler) {
    	this.drtActionCreator = delegate;
    	this.timer = timer;
        this.passengerHandler = passengerHandler;
    }

    @Override
    public DynAction createAction(DynAgent dynAgent, DvrpVehicle vehicle, double now) {

        Task task = vehicle.getSchedule().getCurrentTask();
        if (task instanceof EDrtShiftBreakTaskImpl && ((EDrtShiftBreakTaskImpl) task).getChargingTask() != null) {
            task.initTaskTracker(new OfflineETaskTracker((EvDvrpVehicle) vehicle, timer));
			EDrtShiftBreakTaskImpl t = (EDrtShiftBreakTaskImpl)task;
            return new ChargingBreakActivity(((EDrtShiftBreakTaskImpl) task).getChargingTask(), passengerHandler, dynAgent, t, t.getDropoffRequests(), t.getPickupRequests());
        } else if (task instanceof EDrtShiftChangeoverTaskImpl && ((EDrtShiftChangeoverTaskImpl) task).getChargingTask() != null) {
            task.initTaskTracker(new OfflineETaskTracker((EvDvrpVehicle) vehicle, timer));
            DrtStopTask t = (DrtStopTask) task;
            return new ChargingChangeoverActivity(((EDrtShiftChangeoverTaskImpl) task).getChargingTask(), passengerHandler, dynAgent, t, t.getDropoffRequests(), t.getPickupRequests());
        } else if (task instanceof EDrtWaitForShiftStayTask && ((EDrtWaitForShiftStayTask) task).getChargingTask() != null) {
            task.initTaskTracker(new OfflineETaskTracker((EvDvrpVehicle) vehicle, timer));
            return new ChargingWaitForShiftActivity(((EDrtWaitForShiftStayTask) task).getChargingTask());
        }

        DynAction dynAction = drtActionCreator.createAction(dynAgent, vehicle, now);
        if (task.getTaskTracker() == null) {
            task.initTaskTracker(new OfflineETaskTracker((EvDvrpVehicle) vehicle, timer));
        }
        return dynAction;
    }

    private static VrpLeg createLeg(String mobsimMode, DvrpVehicle vehicle, MobsimTimer timer) {
        DriveTask driveTask = (DriveTask) vehicle.getSchedule().getCurrentTask();
        VrpLeg leg = new VrpLeg(mobsimMode, driveTask.getPath());
        OnlineDriveTaskTracker onlineTracker = new OnlineDriveTaskTrackerImpl(vehicle, leg,
                OnlineTrackerListener.NO_LISTENER, timer);
        OnlineEDriveTaskTracker onlineETracker = new OnlineEDriveTaskTracker((EvDvrpVehicle) vehicle, timer,
                onlineTracker);
        TaskTrackers.initOnlineDriveTaskTracking(vehicle, leg, onlineETracker);
        return leg;
    }
}
