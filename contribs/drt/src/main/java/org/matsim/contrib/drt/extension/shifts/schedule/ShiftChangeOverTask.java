package org.matsim.contrib.drt.extension.shifts.schedule;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.shifts.shift.DrtShift;
import org.matsim.contrib.drt.schedule.DrtStopTask;

/**
 * @author nkuehnel / MOIA
 */
public interface ShiftChangeOverTask extends DrtStopTask, OperationalStop {

    Link getLink();

    DrtShift getShift();
}
