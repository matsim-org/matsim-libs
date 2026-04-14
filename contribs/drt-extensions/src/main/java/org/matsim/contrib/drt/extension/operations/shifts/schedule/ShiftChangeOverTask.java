package org.matsim.contrib.drt.extension.operations.shifts.schedule;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.evrp.ChargingTask;

import java.util.Optional;

/**
 * Interface representing a task for a shift changeover.
 * Supports dynamically adding or removing charging capabilities.
 *
 * @author nkuehnel / MOIA
 */
public interface ShiftChangeOverTask extends DrtStopTask, OperationalStop {

    Link getLink();

    DrtShift getShift();
}
