package org.matsim.contrib.drt.extension.operations.eshifts.schedule;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.edrt.schedule.EDrtChargingTask;
import org.matsim.contrib.drt.extension.edrt.schedule.EDrtTaskFactoryImpl;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilities;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftBreakTask;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftChangeOverTask;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftDrtTaskFactory;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.WaitForShiftStayTask;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftBreak;
import org.matsim.contrib.drt.schedule.DrtDriveTask;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.drt.schedule.DrtTaskType;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.DefaultStayTask;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.evrp.ChargingTask;
import org.matsim.contrib.evrp.ChargingTaskImpl;
import org.matsim.contrib.evrp.EvDvrpVehicle;
import org.matsim.facilities.Facility;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author nkuehnel / MOIA
 */
public class ShiftEDrtTaskFactoryImpl implements ShiftDrtTaskFactory {

    private final EDrtTaskFactoryImpl delegate;
	private final OperationFacilities operationFacilities;


	public ShiftEDrtTaskFactoryImpl(EDrtTaskFactoryImpl delegate, OperationFacilities operationFacilities) {
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
	public DefaultStayTask createInitialTask(DvrpVehicle vehicle, double beginTime, double endTime, Link link) {
		final Map<Id<Link>, List<OperationFacility>> facilitiesByLink = operationFacilities.getDrtOperationFacilities().values().stream().collect(Collectors.groupingBy(Facility::getLinkId));
		final OperationFacility operationFacility;
		try {
			operationFacility = facilitiesByLink.get(vehicle.getStartLink().getId()).stream().findFirst().orElseThrow((Supplier<Throwable>) () -> new RuntimeException("Vehicles must start at an operation facility!"));
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
		WaitForShiftStayTask waitForShiftStayTask = createWaitForShiftStayTask(vehicle, vehicle.getServiceBeginTime(), vehicle.getServiceEndTime(),
				vehicle.getStartLink(), operationFacility);
		boolean success = operationFacility.register(vehicle.getId());
		if (!success) {
			throw new RuntimeException(String.format("Cannot register vehicle %s at facility %s at start-up. Please check" +
					"facility capacity and initial fleet distribution.", vehicle.getId().toString(), operationFacility.getId().toString()));
		}
		return waitForShiftStayTask;
	}

	@Override
    public ShiftBreakTask createShiftBreakTask(DvrpVehicle vehicle, double beginTime, double endTime, Link link,
                                               DrtShiftBreak shiftBreak, OperationFacility facility) {
        return new EDrtShiftBreakTaskImpl(beginTime, endTime, link, shiftBreak, 0, null, facility);
    }

    @Override
    public ShiftChangeOverTask createShiftChangeoverTask(DvrpVehicle vehicle, double beginTime, double endTime,
														 Link link, DrtShift shift, OperationFacility facility) {
        return new EDrtShiftChangeoverTaskImpl(beginTime, endTime, link, shift, 0, null, facility);
    }

    @Override
    public WaitForShiftStayTask createWaitForShiftStayTask(DvrpVehicle vehicle, double beginTime, double endTime, Link link,
														   OperationFacility facility) {
        return new EDrtWaitForShiftStayTask(beginTime, endTime, link, 0, facility, null);
    }

    public WaitForShiftStayTask createChargingWaitForShiftStayTask(DvrpVehicle vehicle, double beginTime,
                                                           double endTime, Link link, OperationFacility facility,
                                                           double totalEnergy, Charger charger) {
        ChargingTask chargingTask = new ChargingTaskImpl(EDrtChargingTask.TYPE, beginTime, endTime, charger, ((EvDvrpVehicle)vehicle).getElectricVehicle(), totalEnergy);
        return new EDrtWaitForShiftStayTask(beginTime, endTime, link, totalEnergy, facility, chargingTask);
    }

    public EDrtShiftBreakTaskImpl createChargingShiftBreakTask(DvrpVehicle vehicle, double beginTime, double endTime, Link link,
                                                               DrtShiftBreak shiftBreak, Charger charger, double totalEnergy, OperationFacility facility) {
        ChargingTask chargingTask = new ChargingTaskImpl(EDrtChargingTask.TYPE, beginTime, endTime, charger, ((EvDvrpVehicle)vehicle).getElectricVehicle(), totalEnergy);
        return new EDrtShiftBreakTaskImpl(beginTime, endTime, link, shiftBreak, totalEnergy, chargingTask, facility);
    }

    public ShiftChangeOverTask createChargingShiftChangeoverTask(DvrpVehicle vehicle, double beginTime, double endTime,
                                                                 Link link, Charger charger, double totalEnergy,
                                                                 DrtShift shift, OperationFacility facility) {
        ChargingTask chargingTask = new ChargingTaskImpl(EDrtChargingTask.TYPE, beginTime, endTime, charger, ((EvDvrpVehicle)vehicle).getElectricVehicle(), totalEnergy);
        return new EDrtShiftChangeoverTaskImpl(beginTime, endTime, link, shift, totalEnergy, chargingTask, facility);
    }
}
