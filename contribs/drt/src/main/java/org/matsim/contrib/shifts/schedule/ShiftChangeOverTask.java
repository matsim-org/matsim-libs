package org.matsim.contrib.shifts.schedule;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.schedule.Task;

/**
 * @author nkuehnel
 */
public interface ShiftChangeOverTask extends Task, OperationalStop {

    Link getLink();

    double getShiftEndTime();
}
