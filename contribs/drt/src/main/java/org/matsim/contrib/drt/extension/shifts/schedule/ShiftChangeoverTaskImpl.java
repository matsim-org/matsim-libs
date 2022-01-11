package org.matsim.contrib.drt.extension.shifts.schedule;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.shifts.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.shifts.shift.DrtShift;
import org.matsim.contrib.drt.schedule.DefaultDrtStopTask;

/**
 * A task representing stopping and waiting for a new shift.
 * @author nkuehnel / MOIA
 */
public class ShiftChangeoverTaskImpl extends DefaultDrtStopTask implements ShiftChangeOverTask {

	private final DrtShift shift;
	private final OperationFacility facility;

	public ShiftChangeoverTaskImpl(double beginTime, double endTime, Link link, DrtShift shift, OperationFacility facility) {
		super(beginTime, endTime, link);
		this.shift = shift;
		this.facility = facility;
	}

	@Override
	public DrtShift getShift() {
		return shift;
	}

	@Override
	public OperationFacility getFacility() {
		return facility;
	}
}

