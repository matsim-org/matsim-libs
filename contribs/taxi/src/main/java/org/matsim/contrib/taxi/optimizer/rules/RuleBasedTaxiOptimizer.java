/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.taxi.optimizer.rules;

import java.util.*;

import org.matsim.api.core.v01.network.*;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.optimizer.*;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.schedule.*;
import org.matsim.contrib.taxi.schedule.TaxiTask.TaxiTaskType;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.contrib.zone.*;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.util.*;

/**
 * @author michalm
 */
public class RuleBasedTaxiOptimizer extends AbstractTaxiOptimizer {
	private final BestDispatchFinder dispatchFinder;
	private final MobsimTimer timer;

	private final IdleTaxiZonalRegistry idleTaxiRegistry;
	private final UnplannedRequestZonalRegistry unplannedRequestRegistry;

	private final RuleBasedTaxiOptimizerParams params;

	public RuleBasedTaxiOptimizer(TaxiConfigGroup taxiCfg, Fleet fleet, Network network, MobsimTimer timer,
			TravelTime travelTime, TravelDisutility travelDisutility, TaxiScheduler scheduler,
			RuleBasedTaxiOptimizerParams params) {
		this(taxiCfg, fleet, scheduler, network, timer, travelTime, travelDisutility, params,
				new SquareGridSystem(network, params.cellSize));
	}

	public RuleBasedTaxiOptimizer(TaxiConfigGroup taxiCfg, Fleet fleet, TaxiScheduler scheduler, Network network,
			MobsimTimer timer, TravelTime travelTime, TravelDisutility travelDisutility,
			RuleBasedTaxiOptimizerParams params, ZonalSystem zonalSystem) {
		super(taxiCfg, fleet, scheduler, params, new TreeSet<TaxiRequest>(Requests.ABSOLUTE_COMPARATOR), false, false);

		this.params = params;
		this.timer = timer;

		if (taxiCfg.isVehicleDiversion()) {
			// hmmmm, change into warning?? or even allow it (e.g. for empty taxi relocaton)??
			throw new RuntimeException("Diversion is not supported by RuleBasedTaxiOptimizer");
		}

		dispatchFinder = new BestDispatchFinder(scheduler, network, timer, travelTime, travelDisutility);
		idleTaxiRegistry = new IdleTaxiZonalRegistry(zonalSystem, scheduler);
		unplannedRequestRegistry = new UnplannedRequestZonalRegistry(zonalSystem);
	}

	@Override
	protected void scheduleUnplannedRequests() {
		if (isReduceTP()) {
			scheduleIdleVehiclesImpl();// reduce T_P to increase throughput (demand > supply)
		} else {
			scheduleUnplannedRequestsImpl();// reduce T_W (regular NOS)
		}
	}

	public enum Goal {
		MIN_WAIT_TIME, MIN_PICKUP_TIME, DEMAND_SUPPLY_EQUIL;
	};

	private boolean isReduceTP() {
		switch (params.goal) {
			case MIN_PICKUP_TIME:
				return true;

			case MIN_WAIT_TIME:
				return false;

			case DEMAND_SUPPLY_EQUIL:
				int awaitingReqCount = Requests.countRequests(getUnplannedRequests(),
						new Requests.IsUrgentPredicate(timer.getTimeOfDay()));

				return awaitingReqCount > idleTaxiRegistry.getVehicleCount();

			default:
				throw new IllegalStateException();
		}
	}

	// request-initiated scheduling
	private void scheduleUnplannedRequestsImpl() {
		int idleCount = idleTaxiRegistry.getVehicleCount();

		Iterator<TaxiRequest> reqIter = getUnplannedRequests().iterator();
		while (reqIter.hasNext() && idleCount > 0) {
			TaxiRequest req = reqIter.next();

			List<Vehicle> selectedVehs = idleCount > params.nearestVehiclesLimit // we do not want to visit more than a
																					// quarter of zones
					? idleTaxiRegistry.findNearestVehicles(req.getFromLink().getFromNode(), params.nearestVehiclesLimit)
					: idleTaxiRegistry.getVehicles();

			if (selectedVehs.isEmpty()) {
				// no vehicle in the registry is idle ==> return
				// Some vehicles may be not idle because they have been assigned another customer,
				// while for others the time window ends (t1)
				//
				// Their statuses will be updated in this time step (we are just before the sim step,
				// triggered by a MobsimBeforeSimStepEvent). Consequently, they will be
				// removed from this registry, but till then they are there.
				return;
			}

			BestDispatchFinder.Dispatch<TaxiRequest> best = dispatchFinder.findBestVehicleForRequest(req, selectedVehs);

			getScheduler().scheduleRequest(best.vehicle, best.destination, best.path);

			reqIter.remove();
			unplannedRequestRegistry.removeRequest(req);
			idleCount--;
		}
	}

	// vehicle-initiated scheduling
	private void scheduleIdleVehiclesImpl() {
		Iterator<Vehicle> vehIter = idleTaxiRegistry.getVehicles().iterator();
		while (vehIter.hasNext() && !getUnplannedRequests().isEmpty()) {
			Vehicle veh = vehIter.next();

			Link link = ((TaxiStayTask)veh.getSchedule().getCurrentTask()).getLink();
			Iterable<TaxiRequest> selectedReqs = getUnplannedRequests().size() > params.nearestRequestsLimit
					? unplannedRequestRegistry.findNearestRequests(link.getToNode(), params.nearestRequestsLimit)
					: getUnplannedRequests();

			BestDispatchFinder.Dispatch<TaxiRequest> best = dispatchFinder.findBestRequestForVehicle(veh, selectedReqs);

			getScheduler().scheduleRequest(best.vehicle, best.destination, best.path);

			getUnplannedRequests().remove(best.destination);
			unplannedRequestRegistry.removeRequest(best.destination);
		}
	}

	@Override
	public void requestSubmitted(Request request) {
		super.requestSubmitted(request);
		unplannedRequestRegistry.addRequest((TaxiRequest)request);
	}

	@Override
	public void nextTask(Vehicle vehicle) {
		super.nextTask(vehicle);

		Schedule schedule = vehicle.getSchedule();
		if (schedule.getStatus() == ScheduleStatus.COMPLETED) {
			TaxiStayTask lastTask = (TaxiStayTask)Schedules.getLastTask(schedule);
			if (lastTask.getBeginTime() < vehicle.getServiceEndTime()) {
				idleTaxiRegistry.removeVehicle(vehicle);
			}
		} else if (getScheduler().isIdle(vehicle)) {
			idleTaxiRegistry.addVehicle(vehicle);
		} else {
			if (schedule.getCurrentTask().getTaskIdx() != 0) {// not first task
				TaxiTask previousTask = (TaxiTask)Schedules.getPreviousTask(schedule);
				if (isWaitStay(previousTask)) {
					idleTaxiRegistry.removeVehicle(vehicle);
				}
			}
		}
	}

	@Override
	protected boolean doReoptimizeAfterNextTask(TaxiTask newCurrentTask) {
		return isWaitStay(newCurrentTask);
	}

	protected boolean isWaitStay(TaxiTask task) {
		return task.getTaxiTaskType() == TaxiTaskType.STAY;
	}

	protected BestDispatchFinder getDispatchFinder() {
		return dispatchFinder;
	}

	protected IdleTaxiZonalRegistry getIdleTaxiRegistry() {
		return idleTaxiRegistry;
	}
}
