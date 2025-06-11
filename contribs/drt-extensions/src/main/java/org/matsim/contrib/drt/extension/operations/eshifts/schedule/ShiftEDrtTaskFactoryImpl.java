package org.matsim.contrib.drt.extension.operations.eshifts.schedule;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.edrt.schedule.EDrtChargingTask;
import org.matsim.contrib.drt.extension.edrt.schedule.EDrtTaskFactoryImpl;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilities;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.operations.shifts.config.ShiftsParams;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftBreakTask;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftChangeOverTask;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftDrtTaskFactory;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.WaitForShiftTask;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftBreak;
import org.matsim.contrib.drt.schedule.DrtDriveTask;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.drt.schedule.DrtTaskType;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.DefaultStayTask;
import org.matsim.contrib.ev.charging.BatteryCharging;
import org.matsim.contrib.ev.charging.ChargingEstimations;
import org.matsim.contrib.ev.charging.ChargingStrategy;
import org.matsim.contrib.ev.charging.ChargingWithAssignmentLogic;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.contrib.evrp.ChargingTask;
import org.matsim.contrib.evrp.ChargingTaskImpl;
import org.matsim.contrib.evrp.EvDvrpVehicle;
import org.matsim.facilities.Facility;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author nkuehnel / MOIA
 */
public class ShiftEDrtTaskFactoryImpl implements ShiftDrtTaskFactory {

    private final EDrtTaskFactoryImpl delegate;

    private final ShiftsParams shiftsParams;

    private final OperationFacilities operationFacilities;
    private final ChargingInfrastructure chargingInfrastructure;
    private final ChargingStrategy.Factory chargingStrategyFactory;


    public ShiftEDrtTaskFactoryImpl(EDrtTaskFactoryImpl delegate, OperationFacilities operationFacilities,
                                    ShiftsParams shiftsParams, ChargingInfrastructure chargingInfrastructure,
                                    ChargingStrategy.Factory chargingStrategyFactory) {
        this.delegate = delegate;
        this.operationFacilities = operationFacilities;
        this.shiftsParams = shiftsParams;
        this.chargingInfrastructure = chargingInfrastructure;

        this.chargingStrategyFactory = chargingStrategyFactory;
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
        WaitForShiftTask waitForShiftTask = createWaitForShiftStayTask(vehicle, vehicle.getServiceBeginTime(), vehicle.getServiceEndTime(),
                vehicle.getStartLink(), operationFacility);
        boolean success = operationFacility.register(vehicle.getId());
        if (!success) {
            throw new RuntimeException(String.format("Cannot register vehicle %s at facility %s at start-up. Please check" +
                    "facility capacity and initial fleet distribution.", vehicle.getId().toString(), operationFacility.getId().toString()));
        }
        return waitForShiftTask;
    }

    @Override
    public ShiftBreakTask createShiftBreakTask(DvrpVehicle vehicle, double beginTime, double endTime, Link link,
                                               DrtShiftBreak shiftBreak, OperationFacility facility) {
        EvDvrpVehicle evVehicle = (EvDvrpVehicle) vehicle;
        Optional<ChargerWithStrategy> charger = decideChargingAtBreak(facility, evVehicle.getElectricVehicle());
        if (charger.isPresent()) {
            final ChargerWithStrategy chargerImpl = charger.get();
            final double waitTime = ChargingEstimations.estimateMaxWaitTimeForNextVehicle(chargerImpl.charger);
            // If battery is sufficiently charged or if there is a wait, use the standard task.
            double soc = evVehicle.getElectricVehicle().getBattery().getCharge() /
                    evVehicle.getElectricVehicle().getBattery().getCapacity();
            if (soc <= shiftsParams.getChargeDuringBreakThreshold() == waitTime > 0) {
                // Otherwise, create a charging break task.
                ChargingStrategy strategy = chargingStrategyFactory.createStrategy(chargerImpl.charger.getSpecification(), evVehicle.getElectricVehicle());
                double energyCharge = ((BatteryCharging) evVehicle.getElectricVehicle().getChargingPower())
                        .calcEnergyCharged(chargerImpl.charger.getSpecification(), endTime - beginTime);
                ((ChargingWithAssignmentLogic) chargerImpl.charger.getLogic()).assignVehicle(evVehicle.getElectricVehicle(), strategy);
                return createChargingShiftBreakTask(vehicle, beginTime,
                        endTime, link, shiftBreak, chargerImpl.charger, -energyCharge, facility, strategy);
            }
        }
        return new EDrtShiftBreakTaskImpl(beginTime, endTime, link, shiftBreak, 0, null, facility);
    }

    @Override
    public ShiftChangeOverTask createShiftChangeoverTask(DvrpVehicle vehicle, double beginTime, double endTime,
                                                         Link link, DrtShift shift, OperationFacility facility) {
        return new EDrtShiftChangeoverTaskImpl(beginTime, endTime, link, shift, 0, null, facility);
    }

    @Override
    public WaitForShiftTask createWaitForShiftStayTask(DvrpVehicle vehicle, double beginTime, double endTime, Link link,
                                                       OperationFacility facility) {
        return new EDrtWaitForShiftTask(beginTime, endTime, link, 0, facility, null);
    }

    public WaitForShiftTask createChargingWaitForShiftStayTask(DvrpVehicle vehicle, double beginTime,
                                                               double endTime, Link link, OperationFacility facility,
                                                               double totalEnergy, Charger charger, ChargingStrategy strategy) {
        ChargingTask chargingTask = new ChargingTaskImpl(EDrtChargingTask.TYPE, beginTime, endTime, charger, ((EvDvrpVehicle)vehicle).getElectricVehicle(), totalEnergy, strategy);
        return new EDrtWaitForShiftTask(beginTime, endTime, link, totalEnergy, facility, chargingTask);
    }

    private EDrtShiftBreakTaskImpl createChargingShiftBreakTask(DvrpVehicle vehicle, double beginTime, double endTime, Link link,
                                                                DrtShiftBreak shiftBreak, Charger charger, double totalEnergy, OperationFacility facility, ChargingStrategy strategy) {
        ChargingTask chargingTask = new ChargingTaskImpl(EDrtChargingTask.TYPE, beginTime, endTime, charger, ((EvDvrpVehicle)vehicle).getElectricVehicle(), totalEnergy, strategy);
        return new EDrtShiftBreakTaskImpl(beginTime, endTime, link, shiftBreak, totalEnergy, chargingTask, facility);
    }

    private record ChargerWithStrategy(Charger charger, ChargingStrategy strategy) {
    }

    private Optional<ChargerWithStrategy> decideChargingAtBreak(OperationFacility breakFacility, ElectricVehicle electricVehicle) {
        if (chargingInfrastructure != null) {
            List<Id<Charger>> chargerIds = breakFacility.getChargers();
            if (!chargerIds.isEmpty()) {
                Optional<Charger> selectedCharger = chargerIds.stream()
                        .map(id -> chargingInfrastructure.getChargers().get(id))
                        .filter(charger -> shiftsParams.getBreakChargerType().equals(charger.getChargerType()))
                        .min((c1, c2) -> {
                            double waitTime1 = ChargingEstimations.estimateMaxWaitTimeForNextVehicle(c1);
                            double waitTime2 = ChargingEstimations.estimateMaxWaitTimeForNextVehicle(c2);
                            return Double.compare(waitTime1, waitTime2);
                        });
                if (selectedCharger.isPresent()) {
                    ChargingStrategy strategy = chargingStrategyFactory.createStrategy(selectedCharger.get().getSpecification(), electricVehicle);
                    if (strategy.isChargingCompleted()) {
                        return Optional.empty();
                    }
                    return Optional.of(new ChargerWithStrategy(selectedCharger.get(), strategy));
                }
            }
        }
        return Optional.empty();
    }
}
