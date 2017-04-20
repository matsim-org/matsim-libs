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

package playground.michalm.taxi.optimizer.assignment;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.taxi.optimizer.BestDispatchFinder.Dispatch;
import org.matsim.contrib.taxi.optimizer.VehicleData;
import org.matsim.contrib.taxi.optimizer.VehicleData.Entry;
import org.matsim.contrib.taxi.optimizer.assignment.*;
import org.matsim.contrib.taxi.optimizer.assignment.VehicleAssignmentProblem.AssignmentCost;
import org.matsim.contrib.util.PartialSort;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;

import com.google.common.collect.*;

import playground.michalm.ev.data.*;
import playground.michalm.taxi.data.EvrpVehicle;
import playground.michalm.taxi.optimizer.ETaxiOptimizerContext;
import playground.michalm.taxi.optimizer.assignment.AssignmentChargerPlugData.ChargerPlug;
import playground.michalm.taxi.schedule.ETaxiChargingTask;
import playground.michalm.taxi.scheduler.ETaxiScheduler;

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
	private final AssignmentETaxiOptimizerParams params;
	private final EvData evData;
	private final ETaxiToPlugAssignmentCostProvider eAssignmentCostProvider;
	private final VehicleAssignmentProblem<ChargerPlug> eAssignmentProblem;
	private final ETaxiScheduler eScheduler;

	private final Map<Id<Vehicle>, Vehicle> scheduledForCharging;

	public AssignmentETaxiOptimizer(ETaxiOptimizerContext optimContext, AssignmentETaxiOptimizerParams params) {
		super(optimContext, params);
		this.params = params;
		evData = optimContext.evData;
		eScheduler = (ETaxiScheduler)optimContext.scheduler;

		if (optimContext.scheduler.getParams().vehicleDiversion
				&& optimContext.scheduler.getParams().destinationKnown) {
			throw new IllegalArgumentException("Unsupported");
		}

		if (params.socCheckTimeStep % params.reoptimizationTimeStep != 0) {
			throw new RuntimeException("charge-scheduling must be followed up by req-scheduling");
		}

		eAssignmentProblem = new VehicleAssignmentProblem<>(optimContext.travelTime, getRouter(), getBackwardRouter());

		eAssignmentCostProvider = new ETaxiToPlugAssignmentCostProvider(params);

		int plugsCount = evData.getChargers().size() * 2;// TODO
		scheduledForCharging = Maps.newHashMapWithExpectedSize(plugsCount * 2);
	}

	private final boolean chargingTaskRemovalEnabled = true;

	@Override
	public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e) {
		if (isNewDecisionEpoch(e, params.socCheckTimeStep)) {
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
		List<Vehicle> vehiclesWithChargingTasksRemoved = eScheduler.endChargingTaskRemoval();
		for (Vehicle v : vehiclesWithChargingTasksRemoved) {
			if (scheduledForCharging.remove(v.getId()) == null) {
				throw new RuntimeException();
			}
		}
	}

	// if socCheckTimeStep is too small --> small number of idle plugs --> poorer assignments
	protected void scheduleCharging() {
		AssignmentChargerPlugData pData = new AssignmentChargerPlugData(getOptimContext(),
				evData.getChargers().values());
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
			eScheduler.scheduleCharging((EvrpVehicle)a.vehicle, a.destination.charger, a.path);
			if (scheduledForCharging.put(a.vehicle.getId(), a.vehicle) != null) {
				throw new IllegalStateException();
			}
		}
	}

	@Override
	public void nextTask(Vehicle vehicle) {
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

	private VehicleData initVehicleDataForCharging(AssignmentChargerPlugData pData) {
		Iterable<? extends Vehicle> vehiclesBelowMinSocLevel = Iterables
				.filter(getOptimContext().fleet.getVehicles().values(), this::doNeedChargingScheduling);

		// XXX if chargers are heavily used then shorten the planning horizon;
		// (like with undersupply of taxis)
		double chargingPlanningHorizon = 10 * 60;// 10 minutes (should be longer than socCheckTimeStep)
		VehicleData vData = new VehicleData(getOptimContext(), vehiclesBelowMinSocLevel, chargingPlanningHorizon);

		// filter least charged vehicles
		PartialSort<Entry> leastChargedSort = new PartialSort<>(pData.getSize());
		for (Entry e : vData.getEntries()) {
			Battery b = ((EvrpVehicle)e.vehicle).getEv().getBattery();
			leastChargedSort.add(e, b.getSoc());// assumption: all b.capacities are equal
		}

		return new VehicleData(leastChargedSort.retriveKSmallestElements());
	}

	// TODO MIN_RELATIVE_SOC should depend on %idle
	private boolean doNeedChargingScheduling(Vehicle v) {
		Battery b = ((EvrpVehicle)v).getEv().getBattery();
		boolean undercharged = b.getSoc() < params.minRelativeSoc * b.getCapacity();
		return (undercharged && !scheduledForCharging.containsKey(v.getId()));
	}
}
