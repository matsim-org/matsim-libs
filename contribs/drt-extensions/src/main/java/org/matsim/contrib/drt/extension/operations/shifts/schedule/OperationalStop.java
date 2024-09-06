package org.matsim.contrib.drt.extension.operations.shifts.schedule;

import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.dvrp.schedule.Task;

/**
 * @author nkuehnel / MOIA
 */
public interface OperationalStop extends Task {
    OperationFacility getFacility();
}
