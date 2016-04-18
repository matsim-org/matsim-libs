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
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

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
public class FifoOptimizer extends AbstractTaxibusOptimizer {

	private LeastCostPathCalculatorWithCache routerWithCache;
	private LineDispatcher dispatcher;
	private Map<Id<TaxibusLine>, TaxibusVehicleRequestPath> currentRequestPathForLine = new HashMap<>();
	private Map<Id<TaxibusLine>, Double> currentTwMax = new HashMap<>();
	private static final Logger log = Logger.getLogger(FifoOptimizer.class);

	public FifoOptimizer(TaxibusOptimizerContext optimContext, LineDispatcher dispatcher,
			boolean doUnscheduleAwaitingRequests) {
		super(optimContext, doUnscheduleAwaitingRequests);
		this.dispatcher = dispatcher;
		LeastCostPathCalculator router = new Dijkstra(optimContext.scenario.getNetwork(),
				optimContext.travelDisutility, optimContext.travelTime);
		routerWithCache = new DefaultLeastCostPathCalculatorWithCache(router,
		        TimeDiscretizer.OPEN_ENDED_15_MIN);
		for (Id<TaxibusLine> line : this.dispatcher.getLines().keySet()) {
			this.currentRequestPathForLine.put(line, null);
			this.currentTwMax.put(line, null);
		}
	}

	@Override
	protected void scheduleUnplannedRequests() {
		
		
		Set<TaxibusRequest> handledRequests = new HashSet<>();
		for (TaxibusRequest req : unplannedRequests) {
			
			TaxibusLine line = dispatcher.findLineForRequest(req);
			if (line == null){ 
				log.error("rejecting reqeuest" + req.getId() +" f "+req.getPassenger());
				req.setRejected(true);
				handledRequests.add(req);
				continue;
			}
			
			if (this.optimContext.timer.getTimeOfDay()>req.getT0()-3600){
			
			TaxibusVehicleRequestPath requestPath = this.currentRequestPathForLine.get(line.getId());
			if (requestPath == null) {
				Vehicle veh = line.getNextEmptyVehicle();
				if (veh == null) break;
				VrpPathWithTravelData path = calculateVrpPath(veh, req);
				requestPath = new TaxibusVehicleRequestPath(veh, req, path);
				this.currentRequestPathForLine.put(line.getId(), requestPath);
				double twmax = Math.max(req.getT0(), path.getArrivalTime()) + line.getCurrentTwMax();
				this.currentTwMax.put(line.getId(), twmax);

			} else {
				double departureTime = requestPath.getEarliestNextDeparture();
				VrpPathWithTravelData path = calculateFromPickupToPickup(requestPath.getLastPathAdded(), req,
						departureTime);
				requestPath.addRequestAndPath(req, path);
				
			}
			handledRequests.add(req);
			if (line.getCurrentOccupationRate() == requestPath.requests.size()) {
				fillPathWithDropOffsAndSchedule(requestPath, line.getId());
				this.currentRequestPathForLine.put(line.getId(), null);
				this.currentTwMax.put(line.getId(), null);
			}
		}
		}
		unplannedRequests.removeAll(handledRequests);
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
				.get(this.dispatcher.calculateNextHoldingPointForTaxibus(requestPath.vehicle,id));
		VrpPathWithTravelData lastPath = VrpPaths.calcAndCreatePath(requestPath.getLastPathAdded().getToLink(), toLink,
				requestPath.getEarliestNextDeparture(), routerWithCache, optimContext.travelTime);
		requestPath.addPath(lastPath);
        optimContext.scheduler.scheduleRequest(requestPath);

		// in the very end, add path to opposite direction
	}

	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
		super.notifyMobsimBeforeSimStep(e);

		// make sure to send taxis on their way once TwMax is exceeded
		Set<Id<TaxibusLine>> resetTwMax = new HashSet<>();
		for (Entry<Id<TaxibusLine>, Double> entry : this.currentTwMax.entrySet()) {
			if (entry.getValue() == null) continue;
			if (entry.getValue() < e.getSimulationTime()) {
				Id<TaxibusLine> lineId = entry.getKey();
				TaxibusVehicleRequestPath requestPath = this.currentRequestPathForLine.get(lineId);
				
				log.info("bus "+requestPath.vehicle.getId()+" has reached TwMax with "+requestPath.requests.size() + " requests planned");
				fillPathWithDropOffsAndSchedule(requestPath, lineId);
				this.currentRequestPathForLine.put(lineId, null);
				resetTwMax.add(lineId);
			}
		}

		for (Id<TaxibusLine> id : resetTwMax) {
			this.currentTwMax.put(id, null);

		}
		//vehicle balancing tba

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
