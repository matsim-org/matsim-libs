package org.matsim.contrib.drt.extension.shifts.schedule;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.shifts.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.schedule.DefaultDrtStopTask;

/**
 * A task representing stopping and waiting for a new shift.
 * @author nkuehnel / MOIA
 */
public class ShiftChangeoverTaskImpl extends DefaultDrtStopTask implements ShiftChangeOverTask {

	private final double shiftEndTime;
	private final OperationFacility facility;

	public ShiftChangeoverTaskImpl(double beginTime, double endTime, Link link, double shiftEndTime, OperationFacility facility) {
		super(beginTime, endTime, link);
		this.shiftEndTime = shiftEndTime;
		this.facility = facility;
	}

	@Override
	public double getShiftEndTime() {
		return shiftEndTime;
	}

	@Override
	public OperationFacility getFacility() {
		return facility;
	}
}

