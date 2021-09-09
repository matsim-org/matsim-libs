package org.matsim.contrib.shifts.schedule;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.shifts.operationFacilities.OperationFacility;

/**
 * A task representing stopping and waiting for a new shift.
 * @author nkuehnel
 */
public class ShiftChangeoverTaskImpl extends DrtStopTask implements ShiftChangeOverTask {

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

