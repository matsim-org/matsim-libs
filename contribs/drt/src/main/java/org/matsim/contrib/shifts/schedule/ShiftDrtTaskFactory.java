package org.matsim.contrib.shifts.schedule;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.schedule.DrtTaskFactory;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.shifts.operationFacilities.OperationFacility;
import org.matsim.contrib.shifts.shift.ShiftBreak;

/**
 * @author nkuehnel
 */
public interface ShiftDrtTaskFactory extends DrtTaskFactory {

    ShiftBreakTask createShiftBreakTask(DvrpVehicle vehicle, double beginTime, double endTime, Link link,
										ShiftBreak shiftBreak, OperationFacility facility);

    ShiftChangeOverTask createShiftChangeoverTask(DvrpVehicle vehicle, double beginTime, double endTime,
                                                  Link link, double latestArrivalTime, OperationFacility facility);

    WaitForShiftStayTask createWaitForShiftStayTask(DvrpVehicle vehicle, double beginTime, double endTime, Link link,
													OperationFacility facility);
}
