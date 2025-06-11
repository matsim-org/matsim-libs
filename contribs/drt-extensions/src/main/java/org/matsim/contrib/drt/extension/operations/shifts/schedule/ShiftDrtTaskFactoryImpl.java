package org.matsim.contrib.drt.extension.operations.shifts.schedule;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilities;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftBreak;
import org.matsim.contrib.drt.schedule.*;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.DefaultStayTask;
import org.matsim.facilities.Facility;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author nkuehnel, fzwick / MOIA
 */
public class ShiftDrtTaskFactoryImpl implements ShiftDrtTaskFactory {

	private final DrtTaskFactory delegate;
	private final OperationFacilities operationFacilities;

	public ShiftDrtTaskFactoryImpl(DrtTaskFactory delegate, OperationFacilities operationFacilities) {
		this.delegate = delegate;
		this.operationFacilities = operationFacilities;
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
	public WaitForShiftTask createWaitForShiftStayTask(DvrpVehicle vehicle, double beginTime, double endTime,
                                                       Link link, OperationFacility facility) {
		return new WaitForShiftTask(beginTime, endTime, link, facility);
	}

	public DefaultStayTask createInitialTask(DvrpVehicle vehicle, double beginTime, double endTime, Link link) {
		final Map<Id<Link>, List<OperationFacility>> facilitiesByLink = operationFacilities.getDrtOperationFacilities().values().stream().collect(Collectors.groupingBy(Facility::getLinkId));
		final OperationFacility operationFacility;
		try {
			operationFacility = facilitiesByLink.get(vehicle.getStartLink().getId()).stream().findFirst().orElseThrow((Supplier<Throwable>) () -> new RuntimeException("Vehicles must start at an operation facility!"));
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
		WaitForShiftTask waitForShiftStayTask = createWaitForShiftStayTask(vehicle, vehicle.getServiceBeginTime(), vehicle.getServiceEndTime(),
				vehicle.getStartLink(), operationFacility);
		boolean success = operationFacility.register(vehicle.getId());
		if (!success) {
			throw new RuntimeException(String.format("Cannot register vehicle %s at facility %s at start-up. Please check" +
					"facility capacity and initial fleet distribution.", vehicle.getId().toString(), operationFacility.getId().toString()));
		}
		return waitForShiftStayTask;
	}

}
