package org.matsim.contrib.drt.extension.shifts.schedule;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.shifts.shift.DrtShift;

/**
 * @author nkuehnel / MOIA
 */
public interface ShiftChangeOverTask extends OperationalStop {

    Link getLink();

    DrtShift getShift();
}
