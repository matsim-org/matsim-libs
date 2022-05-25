package org.matsim.contrib.drt.extension.shifts.schedule;

import org.matsim.contrib.drt.extension.shifts.operationFacilities.OperationFacility;
import org.matsim.contrib.dvrp.schedule.Task;

/**
 * @author nkuehnel / MOIA
 */
public interface OperationalStop extends Task {
    OperationFacility getFacility();
}
