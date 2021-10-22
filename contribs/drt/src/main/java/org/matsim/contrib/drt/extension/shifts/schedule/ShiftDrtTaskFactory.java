package org.matsim.contrib.drt.extension.shifts.schedule;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.schedule.DrtTaskFactory;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.drt.extension.shifts.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.shifts.shift.DrtShiftBreak;

/**
 * @author nkuehnel / MOIA
 */
public interface ShiftDrtTaskFactory extends DrtTaskFactory {

    ShiftBreakTask createShiftBreakTask(DvrpVehicle vehicle, double beginTime, double endTime, Link link,
                                        DrtShiftBreak shiftBreak, OperationFacility facility);

    ShiftChangeOverTask createShiftChangeoverTask(DvrpVehicle vehicle, double beginTime, double endTime,
                                                  Link link, double latestArrivalTime, OperationFacility facility);

    WaitForShiftStayTask createWaitForShiftStayTask(DvrpVehicle vehicle, double beginTime, double endTime, Link link,
													OperationFacility facility);
}
