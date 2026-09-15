package org.matsim.contrib.drt.extension.operations.shifts.schedule;

import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftBreak;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.ev.charging.ChargingStrategy;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.evrp.ChargingTask;

import java.util.Optional;

/**
 * Interface representing a task for a shift break.
 * Supports dynamically adding or removing charging capabilities.
 *
 * @author nkuehnel / MOIA
 */
public interface ShiftBreakTask extends DrtStopTask, OperationalStop {

    /**
     * @return The shift break associated with this task
     */
    DrtShiftBreak getShiftBreak();
    
    /**
     * @return The charging task if this break includes charging, empty otherwise
     */
    Optional<ChargingTask> getChargingTask();
    
    /**
     * Adds charging capability to this break task
     * 
     * @param chargingTask The charging task to add
     * @return true if charging was added successfully, false otherwise
     */
    boolean addCharging(ChargingTask chargingTask);

}
