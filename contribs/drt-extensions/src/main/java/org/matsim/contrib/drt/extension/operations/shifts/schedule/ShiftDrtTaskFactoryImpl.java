package org.matsim.contrib.drt.extension.operations.shifts.schedule;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftBreak;
import org.matsim.contrib.drt.schedule.*;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;

/**
 * @author nkuehnel, fzwick / MOIA
 */
public class ShiftDrtTaskFactoryImpl implements ShiftDrtTaskFactory {

    private final DrtTaskFactory delegate;

    public ShiftDrtTaskFactoryImpl(DrtTaskFactory delegate) {
        this.delegate = delegate;
    }

    @Override
    public DrtDriveTask createDriveTask(DvrpVehicle vehicle, VrpPathWithTravelData path, DrtTaskType drtTaskType) {
        return delegate.createDriveTask(vehicle, path, drtTaskType);
    }

    @Override
    public DrtStopTask createStopTask(DvrpVehicle vehicle, double beginTime, double endTime, Link link) {
        return delegate.createStopTask(vehicle, beginTime, endTime, link);
    }

    @Override
    public DrtStayTask createStayTask(DvrpVehicle vehicle, double beginTime, double endTime, Link link) {
        return delegate.createStayTask(vehicle, beginTime, endTime, link);
    }

    @Override
    public ShiftBreakTask createShiftBreakTask(DvrpVehicle vehicle, double beginTime, double endTime,
                                               Link link, DrtShiftBreak shiftBreak, OperationFacility facility) {
        return new ShiftBreakTaskImpl(beginTime, endTime, link, shiftBreak, facility);
    }

    @Override
    public ShiftChangeOverTask createShiftChangeoverTask(DvrpVehicle vehicle, double beginTime, double endTime,
                                                         Link link, DrtShift shift, OperationFacility facility) {
        return new ShiftChangeoverTaskImpl(beginTime, endTime, link, shift, facility);
    }

    @Override
    public WaitForShiftStayTask createWaitForShiftStayTask(DvrpVehicle vehicle, double beginTime, double endTime,
                                                           Link link, OperationFacility facility) {
        return new WaitForShiftStayTask(beginTime, endTime, link, facility);
    }
}
