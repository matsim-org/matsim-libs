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

package org.matsim.contrib.taxibus.algorithm.optimizer.sharedTaxi;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.*;
import org.matsim.contrib.taxibus.TaxibusRequest;
import org.matsim.contrib.taxibus.algorithm.optimizer.TaxibusOptimizerContext;
import org.matsim.contrib.taxibus.algorithm.scheduler.TaxibusScheduler;
import org.matsim.contrib.taxibus.algorithm.scheduler.vehreqpath.TaxibusDispatch;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.path.*;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.contrib.util.distance.DistanceUtils;
import org.matsim.core.router.*;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

/**
 * @author jbischoff
 *
 */

public class SharedTaxiDispatchFinder {

	private final TaxibusOptimizerContext optimContext;
	private final TaxibusScheduler scheduler;
	private final MultiNodeDijkstra router;
	private final double maximumDetourFactor;

	// private int sharedRides = 0;
	public SharedTaxiDispatchFinder(TaxibusOptimizerContext optimContext, double maximumDetourFactor) {
		this.optimContext = optimContext;
		this.maximumDetourFactor = maximumDetourFactor;

		this.scheduler = optimContext.scheduler;
		router = new MultiNodeDijkstra(optimContext.scenario.getNetwork(), optimContext.travelDisutility,
				optimContext.travelTime, false);
	}

	public TaxibusDispatch findBestNewVehicleForRequest(TaxibusRequest req, Iterable<? extends Vehicle> vehicles)
	// this one is basically a copy from the taxi package. I'm not entirely happy with this solution, but well.
	{
		double currTime = optimContext.timer.getTimeOfDay();
		Link toLink = req.getFromLink();
		Node toNode = toLink.getFromNode();

		Map<Id<Node>, Vehicle> initialVehicles = new HashMap<>();
		Map<Id<Node>, InitialNode> initialNodes = new HashMap<>();
		for (Vehicle veh : vehicles) {
			LinkTimePair departure = scheduler.getImmediateDiversionOrEarliestIdleness(veh);
			if (departure != null) {

				Node vehNode;
				double delay = departure.time - currTime;
				if (departure.link == toLink) {
					// hack: we are basically there (on the same link), so let's pretend vehNode == toNode
					vehNode = toNode;
				} else {
					vehNode = departure.link.getToNode();

					// simplified, but works for taxis, since pickup trips are short (about 5 mins)
					delay += 1 + toLink.getFreespeed(departure.time);
				}

				InitialNode existingInitialNode = initialNodes.get(vehNode.getId());
				if (existingInitialNode == null || existingInitialNode.initialCost > delay) {
					InitialNode newInitialNode = new InitialNode(vehNode, delay, delay);
					initialNodes.put(vehNode.getId(), newInitialNode);
					initialVehicles.put(vehNode.getId(), veh);
				}
			}
		}

		if (initialNodes.isEmpty()) {
			return null;
		}

		ImaginaryNode fromNodes = MultiNodeDijkstra.createImaginaryNode(initialNodes.values());

		Path path = router.calcLeastCostPath(fromNodes, toNode, currTime, null, null);
		// the calculated path contains real nodes (no imaginary/initial nodes),
		// the time and cost are of real travel (between the first and last real node)
		// (no initial times/costs for imaginary<->initial are included)
		Node fromNode = path.nodes.get(0);
		Vehicle bestVehicle = initialVehicles.get(fromNode.getId());
		LinkTimePair bestDeparture = scheduler.getImmediateDiversionOrEarliestIdleness(bestVehicle);

		VrpPathWithTravelData vrpPath = VrpPaths.createPath(bestDeparture.link, toLink, bestDeparture.time, path,
				optimContext.travelTime);
		return new TaxibusDispatch(bestVehicle, req, vrpPath);
	}

	public TaxibusDispatch findBestVehicleForRequest(TaxibusRequest req, Set<Vehicle> busyVehicles,
			Set<Vehicle> idleVehicles) {
		TaxibusDispatch bestNewPath = findBestNewVehicleForRequest(req, idleVehicles);

		double currTime = optimContext.timer.getTimeOfDay();
		double bestSharedTravelTime = Double.MAX_VALUE;
		double bestNewVehDispatchTime = Double.MAX_VALUE;
		if (bestNewPath != null) {
			bestNewVehDispatchTime = bestNewPath.path.get(0).getTravelTime();
		}
		TaxibusDispatch bestSharedPath = null;
		for (Vehicle veh : busyVehicles) {

			Schedule schedule = veh.getSchedule();
			Set<TaxibusRequest> currentRequests = scheduler.getCurrentlyPlannedRequests(schedule);
			if (currentRequests.size() > 1) {
				throw new IllegalStateException("Not supported by this optimizer");
			}

			TaxibusRequest firstRequest = (TaxibusRequest)currentRequests.toArray()[0];
			double pickup2pickupDist = DistanceUtils.calculateSquaredDistance(firstRequest.getFromLink().getCoord(),
					req.getFromLink().getCoord());
			double firstEuclidDist = DistanceUtils.calculateSquaredDistance(firstRequest.getFromLink().getCoord(),
					firstRequest.getToLink().getCoord());
			if (pickup2pickupDist > firstEuclidDist) {
				continue;
			}

			Path firstToSecondPickup = router.calcLeastCostPath(firstRequest.getFromLink().getToNode(),
					req.getFromLink().getFromNode(), currTime, null, null);
			Path currentDirectPath = router.calcLeastCostPath(req.getFromLink().getToNode(),
					req.getToLink().getFromNode(), currTime + firstToSecondPickup.travelTime, null, null);
			Path firstDirectPath = router.calcLeastCostPath(firstRequest.getFromLink().getToNode(),
					firstRequest.getToLink().getFromNode(), currTime, null, null);
			Path secondPickupToFirstDest = router.calcLeastCostPath(req.getFromLink().getToNode(),
					firstRequest.getToLink().getFromNode(), currTime + firstToSecondPickup.travelTime, null, null);
			Path firstDestToSecondDest = router.calcLeastCostPath(firstRequest.getToLink().getToNode(),
					req.getToLink().getFromNode(),
					currTime + secondPickupToFirstDest.travelTime + firstToSecondPickup.travelTime, null, null);
			Path secondDestToFirstDest = router.calcLeastCostPath(req.getToLink().getToNode(),
					firstRequest.getToLink().getFromNode(),
					currTime + currentDirectPath.travelTime + firstToSecondPickup.travelTime, null, null);

			double way212 = secondPickupToFirstDest.travelTime + firstDestToSecondDest.travelTime;
			double way221 = currentDirectPath.travelTime + secondDestToFirstDest.travelTime;
			if (way212 >= way221) {
				double ttFirstCustomer = firstToSecondPickup.travelTime + secondPickupToFirstDest.travelTime
						+ optimContext.tbcg.getPickupDuration();
				double ttSecondCustomer = way212 + optimContext.tbcg.getDropoffDuration();
				if ((ttFirstCustomer <= maximumDetourFactor * firstDirectPath.travelTime)
						&& (ttSecondCustomer <= maximumDetourFactor
								* (currentDirectPath.travelTime + bestNewVehDispatchTime))) {
					if (way212 + firstToSecondPickup.travelTime < bestSharedTravelTime) {
						TaxibusDispatch dispatch = new TaxibusDispatch(veh, firstRequest,
								VrpPaths.createPath(firstRequest.getFromLink(), req.getFromLink(),
										currTime + optimContext.tbcg.getPickupDuration(), firstToSecondPickup,
										optimContext.travelTime));
						dispatch.addRequestAndPath(req,
								VrpPaths.createPath(req.getFromLink(), firstRequest.getToLink(),
										currTime + firstToSecondPickup.travelTime
												+ 2 * optimContext.tbcg.getPickupDuration(),
										secondPickupToFirstDest, optimContext.travelTime));
						dispatch.addPath(VrpPaths.createPath(firstRequest.getToLink(), req.getToLink(),
								currTime + ttFirstCustomer + optimContext.tbcg.getDropoffDuration(),
								firstDestToSecondDest, optimContext.travelTime));
						if (dispatch.getLastPathAdded().getArrivalTime() < veh.getServiceEndTime()) {
							bestSharedPath = dispatch;
						}
					}
				}
			} else {
				double ttFirstCustomer = firstToSecondPickup.travelTime + way221
						+ optimContext.tbcg.getDropoffDuration();
				double ttSecondCustomer = currentDirectPath.travelTime;
				if ((ttFirstCustomer <= maximumDetourFactor * firstDirectPath.travelTime)
						&& (ttSecondCustomer <= maximumDetourFactor * currentDirectPath.travelTime)) {
					if (way221 + firstToSecondPickup.travelTime < bestSharedTravelTime) {
						TaxibusDispatch dispatch = new TaxibusDispatch(veh, firstRequest,
								VrpPaths.createPath(firstRequest.getFromLink(), req.getFromLink(),
										currTime + optimContext.tbcg.getPickupDuration(), firstToSecondPickup,
										optimContext.travelTime));
						dispatch.addRequestAndPath(req,
								VrpPaths.createPath(req.getFromLink(), req.getToLink(),
										currTime + firstToSecondPickup.travelTime
												+ 2 * optimContext.tbcg.getPickupDuration(),
										currentDirectPath, optimContext.travelTime));
						dispatch.addPath(VrpPaths.createPath(req.getToLink(), firstRequest.getToLink(),
								currTime + firstToSecondPickup.travelTime + currentDirectPath.travelTime
										+ 2 * optimContext.tbcg.getPickupDuration()
										+ optimContext.tbcg.getDropoffDuration(),
								secondDestToFirstDest, optimContext.travelTime));
						bestSharedPath = dispatch;
						if (dispatch.getLastPathAdded().getArrivalTime() < veh.getServiceEndTime()) {
							bestSharedPath = dispatch;
						}
					}
				}

			}

		}
		if (bestSharedPath != null) {
			// Logger.getLogger(getClass()).info("Shared Ride :" + bestSharedPath.vehicle.getId() + " no "+
			// sharedRides++);
		}
		if ((bestSharedPath == null) && (bestNewPath != null)) {
			bestNewPath.addPath(VrpPaths.calcAndCreatePath(req.getFromLink(), req.getToLink(),
					currTime + bestNewVehDispatchTime, router, optimContext.travelTime));
			return bestNewPath;
		} else
			return bestSharedPath;
	}

}
