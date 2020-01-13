/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.contrib.etaxi.optimizer.assignment;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.contrib.etaxi.ETaxiChargingTask;
import org.matsim.contrib.etaxi.ETaxiScheduler;
import org.matsim.contrib.etaxi.optimizer.assignment.AssignmentChargerPlugData.ChargerPlug;
import org.matsim.contrib.ev.dvrp.EvDvrpVehicle;
import org.matsim.contrib.ev.fleet.Battery;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.contrib.locationchoice.router.BackwardFastMultiNodeDijkstraFactory;
import org.matsim.contrib.locationchoice.router.BackwardMultiNodePathCalculator;
import org.matsim.contrib.taxi.optimizer.BestDispatchFinder.Dispatch;
import org.matsim.contrib.taxi.optimizer.VehicleData;
import org.matsim.contrib.taxi.optimizer.assignment.AssignmentDestinationData;
import org.matsim.contrib.taxi.optimizer.assignment.AssignmentRequestInserter;
import org.matsim.contrib.taxi.optimizer.assignment.AssignmentTaxiOptimizer;
import org.matsim.contrib.taxi.optimizer.assignment.VehicleAssignmentProblem;
import org.matsim.contrib.taxi.optimizer.assignment.VehicleAssignmentProblem.AssignmentCost;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.scheduler.TaxiScheduleInquiry;
import org.matsim.contrib.util.PartialSort;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.router.FastAStarEuclideanFactory;
import org.matsim.core.router.FastMultiNodeDijkstraFactory;
import org.matsim.core.router.MultiNodePathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.common.collect.Maps;

/**
 * Main assumptions:
 * <ul>
 * <li>no diversion and destination unknown
 * <li>charging scheduling has higher priority than request scheduling
 * <li>charging scheduling is triggered less frequently than request scheduling
 * </ul>
 * To avoid race conditions / oscillations:
 * <ul>
 * <li>charging scheduling can override planned request-related assignments and planned charging assignments
 * <li>request scheduling can override planned request-related assignments
 * <li>currently executed assignments cannot be interrupted (i.e. diversion is off)
 * <li>since the destination remains unknown till the end of pickup, all schedules end with STAY or PICKUP tasks
 * </ul>
 */
public class AssignmentETaxiOptimizer extends AssignmentTaxiOptimizer {
	public static AssignmentETaxiOptimizer create(EventsManager eventsManager, TaxiConfigGroup taxiCfg, Fleet fleet,
			Network network, MobsimTimer timer, TravelTime travelTime, TravelDisutility travelDisutility,
			ETaxiScheduler eScheduler, ChargingInfrastructure chargingInfrastructure) {
		MultiNodePathCalculator multiNodeRouter = (MultiNodePathCalculator)new FastMultiNodeDijkstraFactory(
				true).createPathCalculator(network, travelDisutility, travelTime);
		BackwardMultiNodePathCalculator backwardMultiNodeRouter = (BackwardMultiNodePathCalculator)new BackwardFastMultiNodeDijkstraFactory(
				true).createPathCalculator(network, travelDisutility, travelTime);
		LeastCostPathCalculator router = new FastAStarEuclideanFactory().createPathCalculator(network, travelDisutility,
				travelTime);
		return new AssignmentETaxiOptimizer(eventsManager, taxiCfg, fleet, timer, travelTime, eScheduler,
				chargingInfrastructure, multiNodeRouter, backwardMultiNodeRouter, router);
	}

	private final AssignmentETaxiOptimizerParams params;
	private final ChargingInfrastructure chargingInfrastructure;
	private final ETaxiToPlugAssignmentCostProvider eAssignmentCostProvider;
	private final VehicleAssignmentProblem<ChargerPlug> eAssignmentProblem;
	private final ETaxiScheduler eScheduler;
	private final Fleet fleet;
	private final MobsimTimer timer;

	private final Map<Id<DvrpVehicle>, DvrpVehicle> scheduledForCharging;

	public AssignmentETaxiOptimizer(EventsManager eventsManager, TaxiConfigGroup taxiCfg, Fleet fleet,
			MobsimTimer timer, TravelTime travelTime, ETaxiScheduler eScheduler,
			ChargingInfrastructure chargingInfrastructure, MultiNodePathCalculator multiNodeRouter,
			BackwardMultiNodePathCalculator backwardMultiNodeRouter, LeastCostPathCalculator router) {
		super(eventsManager, taxiCfg, fleet, eScheduler,
				new AssignmentRequestInserter(fleet, timer, travelTime, eScheduler,
						((AssignmentETaxiOptimizerParams)taxiCfg.getTaxiOptimizerParams()).getAssignmentTaxiOptimizerParams(),
						multiNodeRouter, backwardMultiNodeRouter, router));
		this.params = (AssignmentETaxiOptimizerParams)taxiCfg.getTaxiOptimizerParams();
		this.chargingInfrastructure = chargingInfrastructure;
		this.eScheduler = eScheduler;
		this.fleet = fleet;
		this.timer = timer;

		if (taxiCfg.isVehicleDiversion() && taxiCfg.isDestinationKnown()) {
			throw new IllegalArgumentException("Unsupported");
		}

		if (params.getSocCheckTimeStep() % params.getReoptimizationTimeStep() != 0) {
			throw new RuntimeException("charge-scheduling must be followed up by req-scheduling");
		}

		eAssignmentProblem = new VehicleAssignmentProblem<>(travelTime, multiNodeRouter, backwardMultiNodeRouter);

		eAssignmentCostProvider = new ETaxiToPlugAssignmentCostProvider(params);

		int plugsCount = chargingInfrastructure.getChargers().size() * 2;// TODO
		scheduledForCharging = Maps.newHashMapWithExpectedSize(plugsCount * 2);
	}

	private final boolean chargingTaskRemovalEnabled = true;

	@Override
	public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e) {
		if (isNewDecisionEpoch(e, params.getSocCheckTimeStep())) {
			if (chargingTaskRemovalEnabled) {
				unscheduleAwaitingRequestsAndCharging();
			} else {
				unscheduleAwaitingRequests();
			}
			scheduleCharging();
			setRequiresReoptimization(true);
		}

		super.notifyMobsimBeforeSimStep(e);
	}

	private void unscheduleAwaitingRequestsAndCharging() {
		eScheduler.beginChargingTaskRemoval();
		unscheduleAwaitingRequests();// and charging
		List<DvrpVehicle> vehiclesWithChargingTasksRemoved = eScheduler.endChargingTaskRemoval();
		for (DvrpVehicle v : vehiclesWithChargingTasksRemoved) {
			if (scheduledForCharging.remove(v.getId()) == null) {
				throw new RuntimeException();
			}
		}
	}

	// if socCheckTimeStep is too small --> small number of idle plugs --> poorer assignments
	protected void scheduleCharging() {
		AssignmentDestinationData<ChargerPlug> pData = AssignmentChargerPlugData.create(timer.getTimeOfDay(),
				chargingInfrastructure.getChargers().values());
		if (pData.getSize() == 0) {
			return;
		}

		VehicleData vData = initVehicleDataForCharging(pData);
		if (vData.getSize() == 0) {
			return;
		}

		AssignmentCost<ChargerPlug> cost = eAssignmentCostProvider.getCost(pData, vData);
		List<Dispatch<ChargerPlug>> assignments = eAssignmentProblem.findAssignments(vData, pData, cost);

		for (Dispatch<ChargerPlug> a : assignments) {
			EvDvrpVehicle eTaxi = (EvDvrpVehicle)a.vehicle;
			eScheduler.scheduleCharging(eTaxi, eTaxi.getElectricVehicle(), a.destination.charger, a.path);
			if (scheduledForCharging.put(a.vehicle.getId(), a.vehicle) != null) {
				throw new IllegalStateException();
			}
		}
	}

	@Override
	public void nextTask(DvrpVehicle vehicle) {
		Schedule schedule = vehicle.getSchedule();
		if (schedule.getStatus() == ScheduleStatus.STARTED) {
			if (schedule.getCurrentTask() instanceof ETaxiChargingTask) {
				if (scheduledForCharging.remove(vehicle.getId()) == null) {
					throw new IllegalStateException();
				}
			}
		}

		super.nextTask(vehicle);
	}

	private VehicleData initVehicleDataForCharging(AssignmentDestinationData<ChargerPlug> pData) {
		// XXX if chargers are heavily used then shorten the planning horizon;
		// (like with undersupply of taxis)
		// TODO move it to AssignmentETaxiOptimizerParams
		double chargingPlanningHorizon = 10 * 60;// 10 minutes (should be longer than socCheckTimeStep)
		double maxDepartureTime = timer.getTimeOfDay() + chargingPlanningHorizon;
		@SuppressWarnings("unchecked")
		Stream<EvDvrpVehicle> vehiclesBelowMinSocLevel = ((Stream<EvDvrpVehicle>)fleet.getVehicles()
				.values()
				.stream()).filter(v -> isChargingSchedulable(v, eScheduler, maxDepartureTime));

		// filter least charged vehicles
		// assumption: all b.capacities are equal
		List<EvDvrpVehicle> leastChargedVehicles = PartialSort.kSmallestElements(pData.getSize(),
				vehiclesBelowMinSocLevel, v -> v.getElectricVehicle().getBattery().getSoc());

		return new VehicleData(timer.getTimeOfDay(), eScheduler, leastChargedVehicles.stream());
	}

	// TODO MIN_RELATIVE_SOC should depend on %idle
	private boolean isChargingSchedulable(EvDvrpVehicle eTaxi, TaxiScheduleInquiry scheduleInquiry,
			double maxDepartureTime) {
		Battery b = eTaxi.getElectricVehicle().getBattery();
		boolean undercharged = b.getSoc() < params.getMinRelativeSoc() * b.getCapacity();
		if (!undercharged || !scheduledForCharging.containsKey(eTaxi.getId())) {
			return false;// not needed or already planned
		}

		LinkTimePair departure = scheduleInquiry.getImmediateDiversionOrEarliestIdleness(eTaxi);
		return departure != null && departure.time <= maxDepartureTime;// schedulable within the time horizon
	}
}
