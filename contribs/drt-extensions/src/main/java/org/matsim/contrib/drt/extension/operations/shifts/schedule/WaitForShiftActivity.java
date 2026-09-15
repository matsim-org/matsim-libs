package org.matsim.contrib.drt.extension.operations.shifts.schedule;

import org.matsim.contrib.dynagent.DynActivity;
import org.matsim.contrib.dynagent.IdleDynActivity;
import org.matsim.contrib.ev.charging.ChargingWithAssignmentLogic;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.evrp.ChargingActivity;
import org.matsim.contrib.evrp.ChargingTask;

/**
 * A dynamic version of ChargingWaitForShiftActivity that can detect when charging
 * is added to the underlying task during simulation.
 * 
 * @author nkuehnel / MOIA
 */
public class WaitForShiftActivity implements DynActivity {
    public static final String ACTIVITY_TYPE = "Wait for shift";
    
    private final IdleDynActivity idleDynActivity;
    private final WaitForShiftTask waitTask;
    
    private ChargingActivity chargingDelegate;
    private ChargingTask chargingTask;

    public WaitForShiftActivity(WaitForShiftTask waitTask) {
        this.waitTask = waitTask;
        this.idleDynActivity = new IdleDynActivity(ACTIVITY_TYPE, waitTask::getEndTime);
        
        // Initialize with existing charging if any
        waitTask.getChargingTask().ifPresent(this::initializeCharging);
    }

    @Override
    public String getActivityType() {
        return ACTIVITY_TYPE;
    }

    @Override
    public double getEndTime() {
        return idleDynActivity.getEndTime();
    }

    @Override
    public void doSimStep(double now) {
        // Check if task now has charging that wasn't there before
        if (chargingDelegate == null) {
            waitTask.getChargingTask().ifPresent(this::initializeCharging);
        }
        
        // Always execute idle activity
        idleDynActivity.doSimStep(now);
        
        // Execute charging if present
        if (chargingDelegate != null) {
            chargingDelegate.doSimStep(now);
        }
    }

    public void finalizeAction(double now) {
        if (chargingDelegate != null && chargingDelegate.getEndTime() > now) {
            ChargingWithAssignmentLogic logic = chargingTask.getChargingLogic();
            ElectricVehicle ev = chargingTask.getElectricVehicle();
            logic.removeVehicle(ev, now);
        }
    }
    
    private void initializeCharging(ChargingTask task) {
        this.chargingTask = task;
        this.chargingDelegate = new ChargingActivity(task);
    }
}