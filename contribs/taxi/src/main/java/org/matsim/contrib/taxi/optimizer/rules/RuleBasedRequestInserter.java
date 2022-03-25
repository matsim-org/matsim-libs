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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.contrib.taxi.optimizer.BestDispatchFinder;
import org.matsim.contrib.taxi.optimizer.UnplannedRequestInserter;
import org.matsim.contrib.taxi.passenger.TaxiRequest;
import org.matsim.contrib.taxi.schedule.TaxiStayTask;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

/**
 * @author michalm
 */
public class RuleBasedRequestInserter implements UnplannedRequestInserter {
	private static final Logger log = Logger.getLogger(RuleBasedRequestInserter.class);
	private final TaxiScheduler scheduler;
	private final BestDispatchFinder dispatchFinder;
	private final MobsimTimer timer;

	private final IdleTaxiZonalRegistry idleTaxiRegistry;
	private final UnplannedRequestZonalRegistry unplannedRequestRegistry;
	private final DriverConfirmationRegistry driverConfirmationRegistry;

	private final RuleBasedTaxiOptimizerParams params;

	public RuleBasedRequestInserter(TaxiScheduler scheduler, MobsimTimer timer, Network network, TravelTime travelTime,
			TravelDisutility travelDisutility, RuleBasedTaxiOptimizerParams params, ZonalRegisters zonalRegisters,
			DriverConfirmationRegistry driverConfirmationRegistry) {
		this(scheduler, timer,
				new BestDispatchFinder(scheduler.getScheduleInquiry(), network, timer, travelTime, travelDisutility),
				params, zonalRegisters, driverConfirmationRegistry);
	}

	public RuleBasedRequestInserter(TaxiScheduler scheduler, MobsimTimer timer, BestDispatchFinder dispatchFinder,
			RuleBasedTaxiOptimizerParams params, ZonalRegisters zonalRegisters,
			DriverConfirmationRegistry driverConfirmationRegistry) {
		this.scheduler = scheduler;
		this.timer = timer;
		this.params = params;
		this.dispatchFinder = dispatchFinder;
		this.idleTaxiRegistry = zonalRegisters.idleTaxiRegistry;
		this.unplannedRequestRegistry = zonalRegisters.unplannedRequestRegistry;
		this.driverConfirmationRegistry = driverConfirmationRegistry;
	}

	@Override
	public void scheduleUnplannedRequests(Collection<TaxiRequest> unplannedRequests) {
		driverConfirmationRegistry.updateForCurrentTime();
		double now = timer.getTimeOfDay();
		long awaitingReqCount = unplannedRequests.stream()
				.filter(r -> ((PassengerRequest)r).getEarliestStartTime() <= now)//urgent requests
				.count();
		log.warn("CTudorache scheduleUnplannedRequests, goal: " + params.getGoal()
				+ ", awaitingReqCount: " + awaitingReqCount
				+ ", idleTaxi: " + idleTaxiRegistry.getVehicleCount());
		if (isReduceTP(unplannedRequests)) {
			scheduleIdleVehiclesImpl(unplannedRequests);// reduce T_P to increase throughput (demand > supply)
		} else {
			scheduleUnplannedRequestsImpl(unplannedRequests);// reduce T_W (regular NOS)
		}

		// TODO(CTudorache) call expireRequests based on config param (should be enabled)
		expireUnplannedOldRequests(unplannedRequests, now);
	}

	public enum Goal {
		MIN_WAIT_TIME, MIN_PICKUP_TIME, DEMAND_SUPPLY_EQUIL
	}

	private boolean isReduceTP(Collection<TaxiRequest> unplannedRequests) {
		switch (params.getGoal()) {
			case MIN_PICKUP_TIME:
				return true;

			case MIN_WAIT_TIME:
				return false;

			case DEMAND_SUPPLY_EQUIL:
				double now = timer.getTimeOfDay();
				long awaitingReqCount = unplannedRequests.stream()
						.filter(r -> ((PassengerRequest)r).getEarliestStartTime() <= now)//urgent requests
						.count();
				return awaitingReqCount > idleTaxiRegistry.getVehicleCount();

			default:
				throw new IllegalStateException();
		}
	}

	// request-initiated scheduling
	private void scheduleUnplannedRequestsImpl(Collection<TaxiRequest> unplannedRequests) {
		log.warn("CTudorache scheduleUnplannedRequestsImpl #" + unplannedRequests.size());
		// vehicles are not immediately removed so calculate 'idleCount' locally
		int idleCount = idleTaxiRegistry.getVehicleCount();
		int nearestVehiclesLimit = params.getNearestVehiclesLimit();

		Iterator<TaxiRequest> reqIter = unplannedRequests.iterator();
		while (reqIter.hasNext() && idleCount > 0) {
			TaxiRequest req = reqIter.next();
			DriverConfirmation dc = driverConfirmationRegistry.getDriverConfirmation(req);
			if (dc == null) {
				// no confirmation requested from any driver => find a driver and ask for confirmation

				// TODO(CTudorache): simplify, always call idleTaxiRegistry.findNearestVehicles which already contains test for minCount > idleCount
				Stream<DvrpVehicle> selectedVehs = idleCount > nearestVehiclesLimit ?
						idleTaxiRegistry.findNearestVehicles(req.getFromLink().getFromNode(), nearestVehiclesLimit) :
						idleTaxiRegistry.vehicles();
				selectedVehs = selectedVehs.filter(v -> !driverConfirmationRegistry.isWaitingDriverConfirmation(v));

				log.warn("CTudorache scheduleUnplannedRequestsImpl req: " + req + ", selectedVehs: ");
				selectedVehs = selectedVehs.peek(v -> {
					log.warn(" - " + v);
				});

				BestDispatchFinder.Dispatch<TaxiRequest> best = dispatchFinder.findBestVehicleForRequest(req, selectedVehs);
				log.warn("CTudorache best dispatch: " + best);
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

				dc = driverConfirmationRegistry.addDriverConfirmation(req, best.vehicle, best.path);
			}

			log.warn("CTudorache scheduleUnplannedRequestsImpl, waitingDriverConfirmation: " + dc);
			if (!dc.isComplete()) {
				// req is still unplanned, but it was already sent to a driver who is yet to confirm it
				continue;
			}
			if (!dc.isAccepted()) {
				throw new RuntimeException("DriverConfirmation rejection NOT-IMPLEMENTED");
			}

			// req is accepted => schedule it
			driverConfirmationRegistry.removeDriverConfirmation(dc);
			scheduler.scheduleRequest(dc.vehicle, dc.request, dc.getPathToPickup(timer.getTimeOfDay()));

			log.warn("CTudorache req planned, removing from unplanned");
			reqIter.remove();
			unplannedRequestRegistry.removeRequest(req);
			idleCount--;
		}
	}

	// vehicle-initiated scheduling
	private void scheduleIdleVehiclesImpl(Collection<TaxiRequest> unplannedRequests) {
		log.warn("CTudorache scheduleIdleVehiclesImpl, req: #" + unplannedRequests.size() + ", idleTaxiRegistry: " + idleTaxiRegistry.vehicles().count());
		int nearestRequestsLimit = params.getNearestRequestsLimit();
		Iterator<DvrpVehicle> vehIter = idleTaxiRegistry.vehicles().iterator();
		while (vehIter.hasNext() && !unplannedRequests.isEmpty()) {
			DvrpVehicle veh = vehIter.next();

			DriverConfirmation dc = driverConfirmationRegistry.getDriverConfirmation(veh);
			if (dc == null) {
				// no confirmation requested from this driver => find a req and ask for driver confirmation

				Link link = ((TaxiStayTask) veh.getSchedule().getCurrentTask()).getLink();

				Stream<TaxiRequest> selectedReqs = unplannedRequests.size() > nearestRequestsLimit ?
						unplannedRequestRegistry.findNearestRequests(link.getToNode(), nearestRequestsLimit) :
						unplannedRequests.stream();

				BestDispatchFinder.Dispatch<TaxiRequest> best = dispatchFinder.findBestRequestForVehicle(veh, selectedReqs);
				log.warn("CTudorache scheduleIdleVehiclesImpl best dispatch: " + best);

				dc = driverConfirmationRegistry.addDriverConfirmation(best.destination, best.vehicle, best.path);
			}

			log.warn("CTudorache scheduleIdleVehiclesImpl, waitingDriverConfirmation: " + dc);
			if (!dc.isComplete()) {
				// req is still unplanned, but it was already sent to a driver who is yet to confirm it
				continue;
			}
			if (!dc.isAccepted()) {
				throw new RuntimeException("DriverConfirmation rejection NOT-IMPLEMENTED");
			}

			// req is accepted => schedule it
			driverConfirmationRegistry.removeDriverConfirmation(dc);
			scheduler.scheduleRequest(dc.vehicle, dc.request, dc.getPathToPickup(timer.getTimeOfDay()));

			log.warn("CTudorache req planned, removing from unplanned");
			unplannedRequests.remove(dc.request);
			unplannedRequestRegistry.removeRequest(dc.request);
		}
	}

	private void expireUnplannedOldRequests(Collection<TaxiRequest> unplannedRequests, double now) {
		Iterator<TaxiRequest> reqIter = unplannedRequests.iterator();
		while (reqIter.hasNext()) {
			TaxiRequest req = reqIter.next();
			if (req.getLatestStartTime() < now) {
				log.warn("ExpiredRequest: " + req);
				reqIter.remove();
				scheduler.requestExpired(req);
				driverConfirmationRegistry.removeDriverConfirmation(req);
			}
		}
	}
}
