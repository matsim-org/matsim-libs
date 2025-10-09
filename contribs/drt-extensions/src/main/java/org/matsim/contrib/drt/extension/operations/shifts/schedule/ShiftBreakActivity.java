package org.matsim.contrib.drt.extension.operations.shifts.schedule;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.extension.operations.shifts.charging.FixedTimeChargingActivity;
import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.drt.passenger.DrtStopActivity;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerHandler;
import org.matsim.contrib.dynagent.DynActivity;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.contrib.ev.charging.ChargingWithAssignmentLogic;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.evrp.ChargingActivity;
import org.matsim.contrib.evrp.ChargingTask;

import java.util.Map;

/**
 * based on {@link DrtStopActivity} and {@link ChargingActivity}
 * @author nkuehnel / MOIA
 */
public class ShiftBreakActivity implements DynActivity {

    public static final String ACTIVITY_TYPE = "Shift break";
    private final ShiftBreakTask shiftBreakTask;

    private final DrtStopActivity busStopDelegate;

    private final double endTime;

    private FixedTimeChargingActivity chargingDelegate;
    private ChargingTask chargingTask;

    public ShiftBreakActivity(PassengerHandler passengerHandler,
                              DynAgent driver, ShiftBreakTask shiftBreakTask,
                              Map<Id<Request>, ? extends AcceptedDrtRequest> dropoffRequests,
                              Map<Id<Request>, ? extends AcceptedDrtRequest> pickupRequests) {
        this.shiftBreakTask = shiftBreakTask;
        endTime = this.shiftBreakTask.getEndTime();
        busStopDelegate = new DrtStopActivity(passengerHandler, driver, () -> endTime, dropoffRequests, pickupRequests, "");

        // Initialize with existing charging if any
        shiftBreakTask.getChargingTask().ifPresent(this::initializeCharging);
	}


    @Override
    public double getEndTime() {
        return Math.max(endTime, busStopDelegate.getEndTime());
    }

    @Override
    public void doSimStep(double now) {

        if (chargingDelegate == null) {
            shiftBreakTask.getChargingTask().ifPresent(this::initializeCharging);
        }

        // Execute charging if present
        if (chargingDelegate != null) {
            chargingDelegate.doSimStep(now);
        }
        busStopDelegate.doSimStep(now);
    }

    @Override
    public void finalizeAction(double now) {
        if (chargingDelegate != null && chargingDelegate.getEndTime() > now) {
            ChargingWithAssignmentLogic logic = chargingTask.getChargingLogic();
            ElectricVehicle ev = chargingTask.getElectricVehicle();
            logic.removeVehicle(ev, now);
        }
    }

    @Override
    public String getActivityType() {
        return ACTIVITY_TYPE;
    }

    private void initializeCharging(ChargingTask task) {
        this.chargingTask = task;
        this.chargingDelegate = new FixedTimeChargingActivity(chargingTask, endTime);
    }
}
