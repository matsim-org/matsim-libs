package org.matsim.contrib.drt.extension.operations.eshifts.schedule;

import org.matsim.contrib.drt.extension.operations.eshifts.charging.ChargingBreakActivity;
import org.matsim.contrib.drt.extension.operations.eshifts.charging.ChargingChangeoverActivity;
import org.matsim.contrib.drt.extension.operations.eshifts.charging.ChargingWaitForShiftActivity;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftDrtActionCreator;
import org.matsim.contrib.drt.schedule.DrtStopTask;
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
 * @author nkuehnel / MOIA
 */
public class ShiftEDrtActionCreator implements DynActionCreator {

    private final ShiftDrtActionCreator delegate;
    private final MobsimTimer timer;
    private final PassengerHandler passengerHandler;

    public ShiftEDrtActionCreator(ShiftDrtActionCreator delegate, MobsimTimer timer, PassengerHandler passengerHandler) {
    	this.delegate = delegate;
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
        } else if (task instanceof EDrtWaitForShiftTask && ((EDrtWaitForShiftTask) task).getChargingTask() != null) {
            task.initTaskTracker(new OfflineETaskTracker((EvDvrpVehicle) vehicle, timer));
            return new ChargingWaitForShiftActivity(((EDrtWaitForShiftTask) task).getChargingTask());
        }

        DynAction dynAction = delegate.createAction(dynAgent, vehicle, now);
        if (task.getTaskTracker() == null) {
            task.initTaskTracker(new OfflineETaskTracker((EvDvrpVehicle) vehicle, timer));
        }
        return dynAction;
    }
}
