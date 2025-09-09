package org.matsim.contrib.drt.extension.operations.eshifts.dispatcher;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.extension.operations.eshifts.fleet.EvShiftDvrpVehicle;
import org.matsim.contrib.drt.extension.operations.eshifts.schedule.EDrtWaitForShiftTask;
import org.matsim.contrib.drt.extension.operations.eshifts.scheduler.EShiftTaskScheduler;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilities;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.operations.shifts.config.ShiftsParams;
import org.matsim.contrib.drt.extension.operations.shifts.dispatcher.DrtShiftDispatcher;
import org.matsim.contrib.drt.extension.operations.shifts.fleet.ShiftDvrpVehicle;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.OperationalStop;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.WaitForShiftTask;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.ev.charging.ChargingEstimations;
import org.matsim.contrib.ev.charging.ChargingStrategy;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.core.gbl.Gbl;

import java.util.List;
import java.util.Optional;

/**
 * @author nkuehnel, fzwick / MOIA
 */
public class EDrtShiftDispatcherImpl implements DrtShiftDispatcher {

	private final EShiftTaskScheduler shiftTaskScheduler;

	private final ChargingInfrastructure chargingInfrastructure;

	private final ShiftsParams drtShiftParams;

	private final OperationFacilities operationFacilities;

	private final DrtShiftDispatcher delegate;

	private final Fleet fleet;

	private final ChargingStrategy.Factory chargingStrategyFactory;

	public EDrtShiftDispatcherImpl(EShiftTaskScheduler shiftTaskScheduler, ChargingInfrastructure chargingInfrastructure,
								   ShiftsParams drtShiftParams, OperationFacilities operationFacilities,
								   DrtShiftDispatcher delegate, Fleet fleet, ChargingStrategy.Factory chargingStrategyFactory) {
		this.shiftTaskScheduler = shiftTaskScheduler;
		this.chargingInfrastructure = chargingInfrastructure;
		this.drtShiftParams = drtShiftParams;
		this.operationFacilities = operationFacilities;
		this.delegate = delegate;
		this.fleet = fleet;
		this.chargingStrategyFactory = chargingStrategyFactory;
	}

	@Override
	public void initialize() {
		delegate.initialize();
	}

	@Override
	public void startOperationalTask(ShiftDvrpVehicle vehicle, OperationalStop operationalStop) {
		delegate.startOperationalTask(vehicle, operationalStop);
	}

	@Override
	public void endOperationalTask(ShiftDvrpVehicle vehicle, OperationalStop operationalStop) {
		delegate.endOperationalTask(vehicle, operationalStop);
	}

	@Override
	public void dispatch(double timeStep) {
		delegate.dispatch(timeStep);
		checkChargingAtHub(timeStep);
	}

	private void checkChargingAtHub(double timeStep) {
		for (OperationFacility operationFacility : operationFacilities.getFacilities().values()) {
			List<Id<Charger>> chargerIds = operationFacility.getChargers();
			if (chargerIds.isEmpty()) {
				//facility does not have a charger
				continue;
			}
			for (Id<DvrpVehicle> vehicleId: operationFacility.getRegisteredVehicles()) {
				DvrpVehicle dvrpVehicle = fleet.getVehicles().get(vehicleId);
				if (dvrpVehicle instanceof EvShiftDvrpVehicle eShiftVehicle) {
					if (eShiftVehicle.getSchedule().getStatus() == Schedule.ScheduleStatus.STARTED) {
						if(!eShiftVehicle.getShifts().isEmpty()) {
							continue;
						}
						final ElectricVehicle electricVehicle = eShiftVehicle.getElectricVehicle();
						if (electricVehicle.getBattery().getCharge() / electricVehicle.getBattery().getCapacity() < drtShiftParams.getChargeAtHubThreshold()) {
							final Task currentTask = eShiftVehicle.getSchedule().getCurrentTask();
							if (currentTask instanceof EDrtWaitForShiftTask
									&& ((EDrtWaitForShiftTask) currentTask).getChargingTask() == null) {
								Optional<Charger> selectedCharger = chargerIds
										.stream()
										.map(id -> chargingInfrastructure.getChargers().get(id))
										.filter(charger -> drtShiftParams.getOutOfShiftChargerType().equals(charger.getChargerType()))
										.min((c1, c2) -> {
											final double waitTime = ChargingEstimations
													.estimateMaxWaitTimeForNextVehicle(c1);
											final double waitTime2 = ChargingEstimations
													.estimateMaxWaitTimeForNextVehicle(c2);
											return Double.compare(waitTime, waitTime2);
										});

								if (selectedCharger.isPresent()) {
									Charger selectedChargerImpl = selectedCharger.get();
									ChargingStrategy chargingStrategy = chargingStrategyFactory.createStrategy(selectedChargerImpl.getSpecification(), electricVehicle);
									if (!chargingStrategy.isChargingCompleted()) {
										final double waitTime = ChargingEstimations
												.estimateMaxWaitTimeForNextVehicle(selectedChargerImpl);
										final double chargingTime = chargingStrategy
												.calcRemainingTimeToCharge();
										double energy = -chargingStrategy
												.calcRemainingEnergyToCharge();
										final double endTime = timeStep + waitTime + chargingTime;
										if (endTime < currentTask.getEndTime()) {
											shiftTaskScheduler.chargeAtHub((WaitForShiftTask) currentTask, eShiftVehicle,
													electricVehicle, selectedChargerImpl, timeStep, endTime, energy, chargingStrategy);
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}
}
