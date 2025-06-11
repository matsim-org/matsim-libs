package org.matsim.contrib.drt.extension.operations.shifts.schedule;

import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.drt.passenger.DrtStopActivity;
import org.matsim.contrib.dvrp.passenger.PassengerHandler;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic.DynActionCreator;
import org.matsim.contrib.dynagent.DynAction;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.contrib.dynagent.IdleDynActivity;

/**
 * @author nkuehnel / MOIA
 */
public class ShiftDrtActionCreator implements DynActionCreator {

    public static final String DRT_SHIFT_BREAK_NAME = "DrtShiftBreak";
    public static final String DRT_SHIFT_CHANGEOVER_NAME = "DrtShiftChangeover";
    public static final String DRT_SHIFT_WAIT_FOR_SHIFT_NAME = "DrtWaitForShift";

    private final PassengerHandler passengerHandler;
    private final DynActionCreator dynActionCreator;

    public ShiftDrtActionCreator(PassengerHandler passengerHandler, DynActionCreator delegate) {
        this.passengerHandler = passengerHandler;
        dynActionCreator = delegate;
    }

    @Override
    public DynAction createAction(DynAgent dynAgent, DvrpVehicle vehicle, double now) {
        Task task = vehicle.getSchedule().getCurrentTask();
        if (task instanceof ShiftBreakTask) {
            DrtStopTask t = (DrtStopTask)task;
            return new DrtStopActivity(passengerHandler, dynAgent, t::getEndTime, t.getDropoffRequests(), t.getPickupRequests(),
                    DRT_SHIFT_BREAK_NAME);
        } else if (task instanceof ShiftChangeOverTask) {
            DrtStopTask t = (DrtStopTask) task;
            return new DrtStopActivity(passengerHandler, dynAgent, t::getEndTime, t.getDropoffRequests(), t.getPickupRequests(),
                    DRT_SHIFT_CHANGEOVER_NAME);
        } else if (task instanceof WaitForShiftTask) {
            return new IdleDynActivity(DRT_SHIFT_WAIT_FOR_SHIFT_NAME, task::getEndTime);
        } else {
            return dynActionCreator.createAction(dynAgent, vehicle, now);
        }
    }
}
