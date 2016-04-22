/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.jbischoff.taxibus.algorithm.optimizer.fifo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.router.DefaultLeastCostPathCalculatorWithCache;
import org.matsim.contrib.dvrp.router.LeastCostPathCalculatorWithCache;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.contrib.dvrp.util.TimeDiscretizer;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.jbischoff.taxibus.algorithm.optimizer.AbstractTaxibusOptimizer;
import playground.jbischoff.taxibus.algorithm.optimizer.TaxibusOptimizerContext;
import playground.jbischoff.taxibus.algorithm.optimizer.fifo.Lines.LineDispatcher;
import playground.jbischoff.taxibus.algorithm.optimizer.fifo.Lines.TaxibusLine;
import playground.jbischoff.taxibus.algorithm.passenger.TaxibusRequest;
import playground.jbischoff.taxibus.algorithm.scheduler.vehreqpath.TaxibusVehicleRequestPath;

/**
 * @author jbischoff
 *
 */
public class MultipleFifoOptimizer extends AbstractTaxibusOptimizer {

	private LeastCostPathCalculatorWithCache routerWithCache;
	private LineDispatcher dispatcher;
	private Map<Id<TaxibusLine>, Set<TaxibusVehicleRequestPath>> currentRequestPathsForLine = new HashMap<>();
	private static final Logger log = Logger.getLogger(MultipleFifoOptimizer.class);
	private int twmaxdepartures = 0;
	private int fullBusDepartures = 0;

	public enum CostCriteria {
		ARRIVALTIME, BEELINE
	}

	private CostCriteria cost;

	public MultipleFifoOptimizer(TaxibusOptimizerContext optimContext, LineDispatcher dispatcher,
			boolean doUnscheduleAwaitingRequests) {
		super(optimContext, doUnscheduleAwaitingRequests);
		this.dispatcher = dispatcher;
		if (optimContext.tbcg.getDistanceCalculationMode().equals("beeline")) {
			cost = CostCriteria.BEELINE;
		} else if (optimContext.tbcg.getDistanceCalculationMode().equals("earliestArrival")) {
			cost = CostCriteria.ARRIVALTIME;
		} else {
			throw new RuntimeException(
					"No config parameter set for distance comparison, please check and assign in config. Valid parameters are \"beeline\" or \"earliestArrival\". ");

		}

		LeastCostPathCalculator router = new Dijkstra(optimContext.scenario.getNetwork(),
				optimContext.travelDisutility, optimContext.travelTime);
		routerWithCache = new DefaultLeastCostPathCalculatorWithCache(router,
		        TimeDiscretizer.OPEN_ENDED_15_MIN);
		for (Id<TaxibusLine> line : this.dispatcher.getLines().keySet()) {
			this.currentRequestPathsForLine.put(line, new LinkedHashSet<TaxibusVehicleRequestPath>());

		}

	}

	@Override
	protected void scheduleUnplannedRequests() {

		Set<TaxibusRequest> handledRequests = new HashSet<>();
		for (TaxibusRequest req : unplannedRequests) {
			if (req.getT0() < this.optimContext.timer.getTimeOfDay() + 1800)

			{

				Set<TaxibusVehicleRequestPath> setToDepart = new HashSet<>();
				TaxibusLine line = dispatcher.findLineForRequest(req);
				if (line == null) {
					// log.error("rejecting reqeuest" + req.getId() + " f " +
					// req.getPassenger());
					req.setRejected(true);
					handledRequests.add(req);
					continue;
				}

				Set<TaxibusVehicleRequestPath> requestPaths = this.currentRequestPathsForLine.get(line.getId());
				TaxibusVehicleRequestPath best = null;

				VrpPathWithTravelData bestPath = null;
				double bestCriteria = Double.MAX_VALUE;

				// go through existing open paths
				for (TaxibusVehicleRequestPath taxibusVehicleRequestPath : requestPaths) {
					if (taxibusVehicleRequestPath.getEarliestNextDeparture() >= taxibusVehicleRequestPath.getTwMax()) {
						setToDepart.add(taxibusVehicleRequestPath);
						continue;
					}
					double departureTime = taxibusVehicleRequestPath.getEarliestNextDeparture();
					VrpPathWithTravelData path = calculateFromPickupToPickup(
							taxibusVehicleRequestPath.getLastPathAdded(), req, departureTime);
					double currentCriteria = getCriteriaFromPath(path);
					if (req.getT0() > taxibusVehicleRequestPath.getTwMax()) {
						currentCriteria = Double.MAX_VALUE;

						setToDepart.add(taxibusVehicleRequestPath);

					}

					if (currentCriteria < bestCriteria) {
						bestCriteria = currentCriteria;
						best = taxibusVehicleRequestPath;
						bestPath = path;
					}

				}
				// see if it would be better to send out a new vehicle from
				// storage
				if ((requestPaths.size() < line.getMaximumOpenVehicles()) && line.isVehicleInHold()) {

					Link hold = this.optimContext.scenario.getNetwork().getLinks()
							.get(line.getHoldingPosition());
					VrpPathWithTravelData path = VrpPaths.calcAndCreatePath(hold, req.getFromLink(),
							this.optimContext.timer.getTimeOfDay(), routerWithCache, optimContext.travelTime);
					if (getCriteriaFromPath(path) < bestCriteria) {
						Vehicle veh = line.getNextEmptyVehicle();
						best = new TaxibusVehicleRequestPath(veh, req, path);
						double twmax = Math.max(req.getT0(), path.getArrivalTime()) + line.getCurrentTwMax();
						best.setTwMax(twmax);
						handledRequests.add(req);

					} else if (bestPath != null) {
						best.addRequestAndPath(req, bestPath);
						handledRequests.add(req);
					}

				}
				if (best != null) {
					requestPaths.add(best);
					if (line.getCurrentOccupationRate() == best.requests.size()) {
						fillPathWithDropOffsAndSchedule(best, line.getId());
						requestPaths.remove(best);
						fullBusDepartures++;
						log.info(best.vehicle.getId() + " departs fully booked: " + fullBusDepartures);
					}
				}
				for (TaxibusVehicleRequestPath ta : setToDepart) {
					fillPathWithDropOffsAndSchedule(ta, line.getId());
					requestPaths.remove(ta);
					twmaxdepartures++;
					log.info(ta.vehicle.getId() + " exceeds twmax: " + twmaxdepartures);
				}

			}
		}
		unplannedRequests.removeAll(handledRequests);
	}

	private double getCriteriaFromPath(VrpPathWithTravelData path) {
		switch (cost) {
		case BEELINE:
			return CoordUtils.calcEuclideanDistance(path.getFromLink().getCoord(), path.getToLink().getCoord());
		case ARRIVALTIME:
			return path.getArrivalTime();

		}
		return 0;
	}

	private void fillPathWithDropOffsAndSchedule(TaxibusVehicleRequestPath requestPath, Id<TaxibusLine> id) {

		Set<TaxibusRequest> allRequests = new LinkedHashSet<TaxibusRequest>();
		allRequests.addAll(requestPath.requests);

		// sort drop offs in meaningful manner by shortest segment time
		while (!allRequests.isEmpty()) {
			Tuple<VrpPathWithTravelData, TaxibusRequest> nextTuple = getNextDropoffSegment(allRequests,
					requestPath.getEarliestNextDeparture(), requestPath.getLastPathAdded().getToLink());
			requestPath.addPath(nextTuple.getFirst());
			allRequests.remove(nextTuple.getSecond());
		}
		Link toLink = this.optimContext.scenario.getNetwork().getLinks()
				.get(this.dispatcher.calculateNextHoldingPointForTaxibus(requestPath.vehicle, id));
		VrpPathWithTravelData lastPath = VrpPaths.calcAndCreatePath(requestPath.getLastPathAdded().getToLink(), toLink,
				requestPath.getEarliestNextDeparture(), routerWithCache, optimContext.travelTime);
		requestPath.addPath(lastPath);
		optimContext.scheduler.scheduleRequest(requestPath);

		// in the very end, add path to opposite direction
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
		super.notifyMobsimBeforeSimStep(e);

		// make sure to send taxis on their way once TwMax is exceeded
		for (Id<TaxibusLine> lineId : this.currentRequestPathsForLine.keySet()) {
			Set<TaxibusVehicleRequestPath> removedPaths = new HashSet<>();
			Set<TaxibusVehicleRequestPath> currentPathsOnLine = this.currentRequestPathsForLine.get(lineId);
			if (currentPathsOnLine != null) {
				for (TaxibusVehicleRequestPath path : currentPathsOnLine) {
					if (path.getTwMax() < e.getSimulationTime()) {

						log.info("bus " + path.vehicle.getId() + " has reached TwMax with " + path.requests.size()
								+ " requests planned");
						fillPathWithDropOffsAndSchedule(path, lineId);
						removedPaths.add(path);
					}

				}
				currentPathsOnLine.removeAll(removedPaths);
			}
		}

		// vehicle balancing tba

	}

	private VrpPathWithTravelData calculateVrpPath(Vehicle veh, TaxibusRequest req) {
		LinkTimePair departure = optimContext.scheduler.getImmediateDiversionOrEarliestIdleness(veh);
		return departure == null ? //
				null
				: VrpPaths.calcAndCreatePath(departure.link, req.getFromLink(), departure.time, routerWithCache,
						optimContext.travelTime);
	}

	private VrpPathWithTravelData calculateFromPickupToPickup(VrpPathWithTravelData previous, TaxibusRequest current,
			double time) {
		return VrpPaths.calcAndCreatePath(previous.getToLink(), current.getFromLink(), time, routerWithCache,
				optimContext.travelTime);
	}

	private Tuple<VrpPathWithTravelData, TaxibusRequest> getNextDropoffSegment(Set<TaxibusRequest> allRequests,
			double departureTime, Link departureLink) {

		double bestTime = Double.MAX_VALUE;
		Tuple<VrpPathWithTravelData, TaxibusRequest> bestSegment = null;
		for (TaxibusRequest request : allRequests) {
			VrpPathWithTravelData segment = VrpPaths.calcAndCreatePath(departureLink, request.getToLink(),
					departureTime, routerWithCache, optimContext.travelTime);
			if (segment.getTravelTime() < bestTime) {
				bestTime = segment.getTravelTime();
				bestSegment = new Tuple<>(segment, request);

			}
		}

		return bestSegment;
	}

}
