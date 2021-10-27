package org.matsim.contrib.drt.extension.eshifts.schedule;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.schedule.DrtDriveTask;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.drt.schedule.DrtTaskType;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.drt.extension.edrt.schedule.EDrtChargingTask;
import org.matsim.contrib.drt.extension.edrt.schedule.EDrtTaskFactoryImpl;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.evrp.ChargingTask;
import org.matsim.contrib.evrp.ChargingTaskImpl;
import org.matsim.contrib.evrp.EvDvrpVehicle;
import org.matsim.contrib.drt.extension.shifts.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.shifts.schedule.ShiftBreakTask;
import org.matsim.contrib.drt.extension.shifts.schedule.ShiftChangeOverTask;
import org.matsim.contrib.drt.extension.shifts.schedule.ShiftDrtTaskFactory;
import org.matsim.contrib.drt.extension.shifts.schedule.WaitForShiftStayTask;
import org.matsim.contrib.drt.extension.shifts.shift.DrtShiftBreak;

/**
 * @author nkuehnel / MOIA
 */
public class ShiftEDrtTaskFactoryImpl implements ShiftDrtTaskFactory {

    private final EDrtTaskFactoryImpl delegate;

    public ShiftEDrtTaskFactoryImpl(EDrtTaskFactoryImpl delegate) {
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
    public ShiftBreakTask createShiftBreakTask(DvrpVehicle vehicle, double beginTime, double endTime, Link link,
                                               DrtShiftBreak shiftBreak, OperationFacility facility) {
        return new EDrtShiftBreakTaskImpl(beginTime, endTime, link, shiftBreak, 0, null, facility);
    }

    @Override
    public ShiftChangeOverTask createShiftChangeoverTask(DvrpVehicle vehicle, double beginTime, double endTime,
														 Link link, double latestArrivalTime, OperationFacility facility) {
        return new EDrtShiftChangeoverTaskImpl(beginTime, endTime, link, latestArrivalTime, 0, null, facility);
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
                                                                 double latestArrivalTime, OperationFacility facility) {
        ChargingTask chargingTask = new ChargingTaskImpl(EDrtChargingTask.TYPE, beginTime, endTime, charger, ((EvDvrpVehicle)vehicle).getElectricVehicle(), totalEnergy);
        return new EDrtShiftChangeoverTaskImpl(beginTime, endTime, link, latestArrivalTime, totalEnergy, chargingTask, facility);
    }
}
