package org.matsim.contrib.drt.extension.operations.shifts.schedule;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftBreak;
import org.matsim.contrib.drt.schedule.DrtTaskFactory;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

/**
 * @author nkuehnel / MOIA
 */
public interface ShiftDrtTaskFactory extends DrtTaskFactory {

    ShiftBreakTask createShiftBreakTask(DvrpVehicle vehicle, double beginTime, double endTime, Link link,
                                        DrtShiftBreak shiftBreak, OperationFacility facility);

    ShiftChangeOverTask createShiftChangeoverTask(DvrpVehicle vehicle, double beginTime, double endTime,
                                                  Link link, DrtShift shift, OperationFacility facility);

    WaitForShiftTask createWaitForShiftStayTask(DvrpVehicle vehicle, double beginTime, double endTime, Link link,
                                                OperationFacility facility);
}
