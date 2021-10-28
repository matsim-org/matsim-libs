package org.matsim.contrib.drt.extension.shifts.schedule;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.schedule.Task;

/**
 * @author nkuehnel / MOIA
 */
public interface ShiftChangeOverTask extends Task, OperationalStop {

    Link getLink();

    double getShiftEndTime();
}
