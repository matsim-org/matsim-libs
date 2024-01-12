package org.matsim.contrib.drt.extension.operations.eshifts.dispatcher;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.operations.eshifts.fleet.EvShiftDvrpVehicle;
import org.matsim.contrib.drt.extension.operations.eshifts.schedule.EDrtWaitForShiftStayTask;
import org.matsim.contrib.drt.extension.operations.eshifts.scheduler.EShiftTaskScheduler;
import org.matsim.contrib.drt.extension.operations.shifts.config.ShiftsParams;
import org.matsim.contrib.drt.extension.operations.shifts.dispatcher.DrtShiftDispatcher;
import org.matsim.contrib.drt.extension.operations.shifts.fleet.ShiftDvrpVehicle;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilities;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftBreakTask;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.WaitForShiftStayTask;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.ev.charging.ChargingEstimations;
import org.matsim.contrib.ev.charging.ChargingStrategy;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;

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

	public EDrtShiftDispatcherImpl(EShiftTaskScheduler shiftTaskScheduler, ChargingInfrastructure chargingInfrastructure,
								   ShiftsParams drtShiftParams, OperationFacilities operationFacilities,
								   DrtShiftDispatcher delegate, Fleet fleet) {
		this.shiftTaskScheduler = shiftTaskScheduler;
		this.chargingInfrastructure = chargingInfrastructure;
		this.drtShiftParams = drtShiftParams;
		this.operationFacilities = operationFacilities;
		this.delegate = delegate;
		this.fleet = fleet;
	}

	@Override
	public void initialize() {
		delegate.initialize();
	}

	@Override
	public void dispatch(double timeStep) {
		delegate.dispatch(timeStep);
		checkChargingAtHub(timeStep);
	}

	@Override
	public void endShift(ShiftDvrpVehicle vehicle, Id<Link> id, Id<OperationFacility> operationFacilityId) {
		delegate.endShift(vehicle, id, operationFacilityId);
	}

	@Override
	public void endBreak(ShiftDvrpVehicle vehicle, ShiftBreakTask task) {
		delegate.endBreak(vehicle, task);
	}

	@Override
	public void startBreak(ShiftDvrpVehicle vehicle, Id<Link> linkId) {
		delegate.startBreak(vehicle, linkId);
	}

	private void checkChargingAtHub(double timeStep) {
		for (OperationFacility operationFacility : operationFacilities.getDrtOperationFacilities().values()) {
			List<Id<Charger>> chargerIds = operationFacility.getChargers();
			if (chargerIds.isEmpty()) {
				//facility does not have a charger
				continue;
			}
			for (Id<DvrpVehicle> vehicle : operationFacility.getRegisteredVehicles()) {
				DvrpVehicle dvrpVehicle = fleet.getVehicles().get(vehicle);
				if (dvrpVehicle instanceof EvShiftDvrpVehicle eShiftVehicle) {
					if (eShiftVehicle.getSchedule().getStatus() == Schedule.ScheduleStatus.STARTED) {
						if(!eShiftVehicle.getShifts().isEmpty()) {
							continue;
						}
						final ElectricVehicle electricVehicle = eShiftVehicle.getElectricVehicle();
						if (electricVehicle.getBattery().getCharge() / electricVehicle.getBattery().getCapacity() < drtShiftParams.chargeAtHubThreshold) {
							final Task currentTask = eShiftVehicle.getSchedule().getCurrentTask();
							if (currentTask instanceof EDrtWaitForShiftStayTask
									&& ((EDrtWaitForShiftStayTask) currentTask).getChargingTask() == null) {
								Optional<Charger> selectedCharger = chargerIds
										.stream()
										.map(id -> chargingInfrastructure.getChargers().get(id))
										.filter(charger -> drtShiftParams.outOfShiftChargerType.equals(charger.getChargerType()))
										.min((c1, c2) -> {
											final double waitTime = ChargingEstimations
													.estimateMaxWaitTimeForNextVehicle(c1);
											final double waitTime2 = ChargingEstimations
													.estimateMaxWaitTimeForNextVehicle(c2);
											return Double.compare(waitTime, waitTime2);
										});

								if (selectedCharger.isPresent()) {
									Charger selectedChargerImpl = selectedCharger.get();
									ChargingStrategy chargingStrategy = selectedChargerImpl.getLogic().getChargingStrategy();
									if (!chargingStrategy.isChargingCompleted(electricVehicle)) {
										final double waitTime = ChargingEstimations
												.estimateMaxWaitTimeForNextVehicle(selectedChargerImpl);
										final double chargingTime = chargingStrategy
												.calcRemainingTimeToCharge(electricVehicle);
										double energy = -chargingStrategy
												.calcRemainingEnergyToCharge(electricVehicle);
										final double endTime = timeStep + waitTime + chargingTime;
										if (endTime < currentTask.getEndTime()) {
											shiftTaskScheduler.chargeAtHub((WaitForShiftStayTask) currentTask, eShiftVehicle,
													electricVehicle, selectedChargerImpl, timeStep, endTime, energy);
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
