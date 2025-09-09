package org.matsim.contrib.drt.extension.operations.eshifts.schedule;

import com.google.common.base.Verify;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.common.util.reservation.ReservationManager;
import org.matsim.contrib.common.util.reservation.ReservationManager.ReservationInfo;
import org.matsim.contrib.drt.extension.edrt.schedule.EDrtChargingTask;
import org.matsim.contrib.drt.extension.edrt.schedule.EDrtTaskFactoryImpl;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilities;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilityReservationManager;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilityUtils;
import org.matsim.contrib.drt.extension.operations.shifts.config.ShiftsParams;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.*;
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

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * @author nkuehnel / MOIA
 */
public class ShiftEDrtTaskFactoryImpl implements ShiftDrtTaskFactory {

    private final EDrtTaskFactoryImpl delegate;

    private final ShiftsParams shiftsParams;

    private final OperationFacilities operationFacilities;
    private final ChargingInfrastructure chargingInfrastructure;
    private final ChargingStrategy.Factory chargingStrategyFactory;
    private final OperationFacilityReservationManager reservationManager;


    public ShiftEDrtTaskFactoryImpl(EDrtTaskFactoryImpl delegate, OperationFacilities operationFacilities,
                                    ShiftsParams shiftsParams, ChargingInfrastructure chargingInfrastructure,
                                    ChargingStrategy.Factory chargingStrategyFactory, OperationFacilityReservationManager reservationManager) {
        this.delegate = delegate;
        this.operationFacilities = operationFacilities;
        this.shiftsParams = shiftsParams;
        this.chargingInfrastructure = chargingInfrastructure;

        this.chargingStrategyFactory = chargingStrategyFactory;
        this.reservationManager = reservationManager;
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
        try {
            OperationFacility facility = OperationFacilityUtils.getFacilityForLink(operationFacilities, vehicle.getStartLink().getId())
                    .orElseThrow((Supplier<Throwable>) () -> new RuntimeException("Vehicles must start at an operation facility!"));
            Optional<ReservationInfo<OperationFacility, DvrpVehicle>> reservation =
                    reservationManager.addReservation(facility, vehicle, beginTime, endTime);

            Verify.verify(reservation.isPresent(), "Could not register %s vehicle at facility %s during start up!", vehicle.getId(), facility.getId());

            facility.register(vehicle.getId());
            return createWaitForShiftStayTask(vehicle, vehicle.getServiceBeginTime(), vehicle.getServiceEndTime(),
                    vehicle.getStartLink(), facility.getId(), reservation.get().reservationId());


        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ShiftBreakTask createShiftBreakTask(DvrpVehicle vehicle, double beginTime, double endTime, Link link,
                                               DrtShiftBreak shiftBreak, Id<OperationFacility> facilityId,
                                               Id<ReservationManager.Reservation> reservationId) {
        EvDvrpVehicle evVehicle = (EvDvrpVehicle) vehicle;
        OperationFacility operationFacility = operationFacilities.getFacilities().get(facilityId);
        Optional<ChargerWithStrategy> charger = decideChargingAtBreak(operationFacility, evVehicle.getElectricVehicle());
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
                        endTime, link, shiftBreak, chargerImpl.charger, -energyCharge, facilityId, reservationId, strategy);
            }
        }
        return new EDrtShiftBreakTaskImpl(beginTime, endTime, link, shiftBreak, 0, null, facilityId, reservationId);
    }

    @Override
    public ShiftChangeOverTask createShiftChangeoverTask(DvrpVehicle vehicle, double beginTime, double endTime,
                                                         Link link, DrtShift shift, Id<OperationFacility> facilityId,
                                                         Id<ReservationManager.Reservation> reservationId) {
        return new EDrtShiftChangeoverTaskImpl(beginTime, endTime, link, shift, 0, null, facilityId, reservationId);
    }

    @Override
    public WaitForShiftTask createWaitForShiftStayTask(DvrpVehicle vehicle, double beginTime, double endTime, Link link,
                                                       Id<OperationFacility> facilityId,
                                                       Id<ReservationManager.Reservation> reservationId) {
        return new EDrtWaitForShiftTask(beginTime, endTime, link, 0, facilityId, reservationId, null);
    }

    public WaitForShiftTask createChargingWaitForShiftStayTask(DvrpVehicle vehicle, double beginTime,
                                                               double endTime, Link link, Id<OperationFacility> facilityId,
                                                               Id<ReservationManager.Reservation> reservationInfo,
                                                               double totalEnergy, Charger charger, ChargingStrategy strategy) {
        ChargingTask chargingTask = new ChargingTaskImpl(EDrtChargingTask.TYPE, beginTime, endTime, charger, ((EvDvrpVehicle) vehicle).getElectricVehicle(), totalEnergy, strategy);
        return new EDrtWaitForShiftTask(beginTime, endTime, link, totalEnergy, facilityId, reservationInfo, chargingTask);
    }

    private EDrtShiftBreakTaskImpl createChargingShiftBreakTask(DvrpVehicle vehicle, double beginTime, double endTime, Link link,
                                                                DrtShiftBreak shiftBreak, Charger charger, double totalEnergy,
                                                                Id<OperationFacility> facilityId,
                                                                Id<ReservationManager.Reservation> reservationId, ChargingStrategy strategy) {
        ChargingTask chargingTask = new ChargingTaskImpl(EDrtChargingTask.TYPE, beginTime, endTime, charger, ((EvDvrpVehicle) vehicle).getElectricVehicle(), totalEnergy, strategy);
        return new EDrtShiftBreakTaskImpl(beginTime, endTime, link, shiftBreak, totalEnergy, chargingTask, facilityId, reservationId);
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
