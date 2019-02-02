/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Stream;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.DvrpVehicle;
import org.matsim.contrib.dvrp.passenger.PassengerRequests;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.optimizer.BestDispatchFinder;
import org.matsim.contrib.taxi.optimizer.UnplannedRequestInserter;
import org.matsim.contrib.taxi.schedule.TaxiStayTask;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

/**
 * @author michalm
 */
public class RuleBasedRequestInserter implements UnplannedRequestInserter {
	private final TaxiScheduler scheduler;
	private final BestDispatchFinder dispatchFinder;
	private final MobsimTimer timer;

	private final IdleTaxiZonalRegistry idleTaxiRegistry;
	private final UnplannedRequestZonalRegistry unplannedRequestRegistry;

	private final RuleBasedTaxiOptimizerParams params;

	public RuleBasedRequestInserter(TaxiScheduler scheduler, MobsimTimer timer, Network network, TravelTime travelTime,
			TravelDisutility travelDisutility, RuleBasedTaxiOptimizerParams params,
			IdleTaxiZonalRegistry idleTaxiRegistry, UnplannedRequestZonalRegistry unplannedRequestRegistry) {
		this(scheduler, timer, new BestDispatchFinder(scheduler, network, timer, travelTime, travelDisutility), params,
				idleTaxiRegistry, unplannedRequestRegistry);
	}

	public RuleBasedRequestInserter(TaxiScheduler scheduler, MobsimTimer timer, BestDispatchFinder dispatchFinder,
			RuleBasedTaxiOptimizerParams params, IdleTaxiZonalRegistry idleTaxiRegistry,
			UnplannedRequestZonalRegistry unplannedRequestRegistry) {
		this.scheduler = scheduler;
		this.timer = timer;
		this.params = params;
		this.dispatchFinder = dispatchFinder;
		this.idleTaxiRegistry = idleTaxiRegistry;
		this.unplannedRequestRegistry = unplannedRequestRegistry;
	}

	@Override
	public void scheduleUnplannedRequests(Collection<TaxiRequest> unplannedRequests) {
		if (isReduceTP(unplannedRequests)) {
			scheduleIdleVehiclesImpl(unplannedRequests);// reduce T_P to increase throughput (demand > supply)
		} else {
			scheduleUnplannedRequestsImpl(unplannedRequests);// reduce T_W (regular NOS)
		}
	}

	public enum Goal {
		MIN_WAIT_TIME, MIN_PICKUP_TIME, DEMAND_SUPPLY_EQUIL;
	};

	private boolean isReduceTP(Collection<TaxiRequest> unplannedRequests) {
		switch (params.goal) {
			case MIN_PICKUP_TIME:
				return true;

			case MIN_WAIT_TIME:
				return false;

			case DEMAND_SUPPLY_EQUIL:
				double now = timer.getTimeOfDay();
				long awaitingReqCount = unplannedRequests.stream().filter(r -> PassengerRequests.isUrgent(r, now))
						.count();
				return awaitingReqCount > idleTaxiRegistry.getVehicleCount();

			default:
				throw new IllegalStateException();
		}
	}

	// request-initiated scheduling
	private void scheduleUnplannedRequestsImpl(Collection<TaxiRequest> unplannedRequests) {
		// vehicles are not immediately removed so calculate 'idleCount' locally
		int idleCount = idleTaxiRegistry.getVehicleCount();

		Iterator<TaxiRequest> reqIter = unplannedRequests.iterator();
		while (reqIter.hasNext() && idleCount > 0) {
			TaxiRequest req = reqIter.next();

			Stream<DvrpVehicle> selectedVehs = idleCount > params.nearestVehiclesLimit//
					? idleTaxiRegistry.findNearestVehicles(req.getFromLink().getFromNode(), params.nearestVehiclesLimit)
					: idleTaxiRegistry.vehicles();

			BestDispatchFinder.Dispatch<TaxiRequest> best = dispatchFinder.findBestVehicleForRequest(req, selectedVehs);
			if (best == null) {
				// XXX NOTE:
				// There may be no idle vehicle in the registry despite idleCount > 0
				// Some vehicles may not be idle because they have been assigned another customer,
				// while for others the time window ends (t1).
				// Their statuses will be updated in this time step (we are just before the sim step,
				// triggered by a MobsimBeforeSimStepEvent). Consequently, they will be
				// removed from this registry, but till then they are there.
				return;
			}

			scheduler.scheduleRequest(best.vehicle, best.destination, best.path);

			reqIter.remove();
			unplannedRequestRegistry.removeRequest(req);
			idleCount--;
		}
	}

	// vehicle-initiated scheduling
	private void scheduleIdleVehiclesImpl(Collection<TaxiRequest> unplannedRequests) {
		Iterator<DvrpVehicle> vehIter = idleTaxiRegistry.vehicles().iterator();
		while (vehIter.hasNext() && !unplannedRequests.isEmpty()) {
			DvrpVehicle veh = vehIter.next();
			Link link = ((TaxiStayTask)veh.getSchedule().getCurrentTask()).getLink();

			Stream<TaxiRequest> selectedReqs = unplannedRequests.size() > params.nearestRequestsLimit
					? unplannedRequestRegistry.findNearestRequests(link.getToNode(), params.nearestRequestsLimit)
					: unplannedRequests.stream();

			BestDispatchFinder.Dispatch<TaxiRequest> best = dispatchFinder.findBestRequestForVehicle(veh, selectedReqs);

			scheduler.scheduleRequest(best.vehicle, best.destination, best.path);

			unplannedRequests.remove(best.destination);
			unplannedRequestRegistry.removeRequest(best.destination);
		}
	}
}
