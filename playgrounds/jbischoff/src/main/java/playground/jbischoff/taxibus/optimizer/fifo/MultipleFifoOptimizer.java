///* *********************************************************************** *
// * project: org.matsim.*
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2015 by the members listed in the COPYING,        *
// *                   LICENSE and WARRANTY file.                            *
// * email           : info at matsim dot org                                *
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// *   This program is free software; you can redistribute it and/or modify  *
// *   it under the terms of the GNU General Public License as published by  *
// *   the Free Software Foundation; either version 2 of the License, or     *
// *   (at your option) any later version.                                   *
// *   See also COPYING, LICENSE and WARRANTY file                           *
// *                                                                         *
// * *********************************************************************** */
//
//package playground.jbischoff.taxibus.optimizer.fifo;
//
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.LinkedHashSet;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Map;
//import java.util.Map.Entry;
//import java.util.Set;
//import java.util.TreeSet;
//
//import org.apache.log4j.Logger;
//import org.matsim.api.core.v01.Id;
//import org.matsim.api.core.v01.network.Link;
//import org.matsim.contrib.dvrp.data.Vehicle;
//import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
//import org.matsim.contrib.dvrp.path.VrpPaths;
//import org.matsim.contrib.dvrp.router.DefaultLeastCostPathCalculatorWithCache;
//import org.matsim.contrib.dvrp.router.LeastCostPathCalculatorWithCache;
//import org.matsim.contrib.dvrp.util.LinkTimePair;
//import org.matsim.contrib.dvrp.util.TimeDiscretizer;
//import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
//import org.matsim.core.router.Dijkstra;
//import org.matsim.core.router.util.LeastCostPathCalculator;
//import org.matsim.core.utils.collections.Tuple;
//
//import playground.jbischoff.taxibus.optimizer.AbstractTaxibusOptimizer;
//import playground.jbischoff.taxibus.optimizer.TaxibusOptimizerConfiguration;
//import playground.jbischoff.taxibus.optimizer.fifo.Lines.LineDispatcher;
//import playground.jbischoff.taxibus.optimizer.fifo.Lines.TaxibusLine;
//import playground.jbischoff.taxibus.passenger.TaxibusRequest;
//import playground.jbischoff.taxibus.vehreqpath.TaxibusVehicleRequestPath;
//
///**
// * @author jbischoff
// *
// */
//public class MultipleFifoOptimizer extends AbstractTaxibusOptimizer {
//
//	private LeastCostPathCalculatorWithCache routerWithCache;
//	private LineDispatcher dispatcher;
//	private Map<Id<TaxibusLine>, List<TaxibusVehicleRequestPath>> currentRequestPathForLine = new HashMap<>();
//	private static final Logger log = Logger.getLogger(MultipleFifoOptimizer.class);
//
//	public MultipleFifoOptimizer(TaxibusOptimizerConfiguration optimConfig, LineDispatcher dispatcher,
//			boolean doUnscheduleAwaitingRequests) {
//		super(optimConfig, doUnscheduleAwaitingRequests);
//		this.dispatcher = dispatcher;
//		LeastCostPathCalculator router = new Dijkstra(optimConfig.context.getScenario().getNetwork(),
//				optimConfig.travelDisutility, optimConfig.travelTime);
//		routerWithCache = new DefaultLeastCostPathCalculatorWithCache(router,
//				new TimeDiscretizer(30 * 4, 15 * 60, false));
//		for (Id<TaxibusLine> line : this.dispatcher.getLines().keySet()) {
//			this.currentRequestPathForLine.put(line, new LinkedList<TaxibusVehicleRequestPath>());
//			
//		}
//	}
//
//	@Override
//	protected void scheduleUnplannedRequests() {
//		
//		
//		Set<TaxibusRequest> handledRequests = new HashSet<>();
//		for (TaxibusRequest req : unplannedRequests) {
//			
//			TaxibusLine line = dispatcher.findLineForRequest(req);
//			if (line == null){ 
//				log.error("rejecting reqeuest" + req.getId() +" f "+req.getPassenger());
//				req.setRejected(true);
//				handledRequests.add(req);
//				continue;
//			}
//			
//			if (this.optimConfig.context.getTime()>req.getT0()-3600){
//			
//			List<TaxibusVehicleRequestPath> requestPaths = this.currentRequestPathForLine.get(line.getId());
//			if (requestPaths.isEmpty()) {
//				Vehicle veh = line.getNextEmptyVehicle();
//				if (veh == null) break;
//				VrpPathWithTravelData path = calculateVrpPath(veh, req);
//				TaxibusVehicleRequestPath taxibusVehicleRequestPath = new TaxibusVehicleRequestPath(veh, req, path);
//				requestPaths.add(taxibusVehicleRequestPath);
//				this.currentRequestPathForLine.put(line.getId(), requestPaths);
//				double twmax = Math.max(req.getT0(), path.getArrivalTime()) + line.getCurrentTwMax();
//				taxibusVehicleRequestPath.setTwMax(twmax);
//				
//
//			} else {
//				TaxibusVehicleRequestPath best;
//				VrpPathWithTravelData bestPath;
//				double bestArrivaltime = Double.MAX_VALUE;
//				for (TaxibusVehicleRequestPath taxibusVehicleRequestPath : requestPaths){
//					double departureTime = taxibusVehicleRequestPath.getEarliestNextDeparture();
//					VrpPathWithTravelData path = calculateFromPickupToPickup(taxibusVehicleRequestPath.getLastPathAdded(), req,
//							departureTime);
//					if (path.getTravelTime()<bestArrivaltime){
//						bestArrivaltime = path.getTravelTime();
//						best = taxibusVehicleRequestPath;
//						bestPath = path;
//					}
//					
//					
//				}
//			
//				requestPaths.addRequestAndPath(req, path);
//				
//			}
//			handledRequests.add(req);
//			if (line.getCurrentOccupationRate() == requestPaths.requests.size()) {
//				fillPathWithDropOffsAndSchedule(requestPaths, line.getId());
//				this.currentRequestPathForLine.put(line.getId(), null);
//				this.currentTwMax.put(line.getId(), null);
//			}
//		}
//		}
//		unplannedRequests.removeAll(handledRequests);
//	}
//
//	private void fillPathWithDropOffsAndSchedule(TaxibusVehicleRequestPath requestPath, Id<TaxibusLine> id) {
//
//		Set<TaxibusRequest> allRequests = new LinkedHashSet<TaxibusRequest>();
//		allRequests.addAll(requestPath.requests);
//
//		// sort drop offs in meaningful manner by shortest segment time
//		while (!allRequests.isEmpty()) {
//			Tuple<VrpPathWithTravelData, TaxibusRequest> nextTuple = getNextDropoffSegment(allRequests,
//					requestPath.getEarliestNextDeparture(), requestPath.getLastPathAdded().getToLink());
//			requestPath.addPath(nextTuple.getFirst());
//			allRequests.remove(nextTuple.getSecond());
//		}
//		Link toLink = this.optimConfig.context.getScenario().getNetwork().getLinks()
//				.get(this.dispatcher.calculateNextHoldingPointForTaxibus(requestPath.vehicle,id));
//		VrpPathWithTravelData lastPath = VrpPaths.calcAndCreatePath(requestPath.getLastPathAdded().getToLink(), toLink,
//				requestPath.getEarliestNextDeparture(), routerWithCache, optimConfig.travelTime);
//		requestPath.addPath(lastPath);
//        optimConfig.scheduler.scheduleRequest(requestPath);
//
//		// in the very end, add path to opposite direction
//	}
//
//	@SuppressWarnings("rawtypes")
//	@Override
//	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
//		super.notifyMobsimBeforeSimStep(e);
//
//		// make sure to send taxis on their way once TwMax is exceeded
//		Set<Id<TaxibusLine>> resetTwMax = new HashSet<>();
//		for (Entry<Id<TaxibusLine>, Double> entry : this.currentTwMax.entrySet()) {
//			if (entry.getValue() == null) continue;
//			if (entry.getValue() < e.getSimulationTime()) {
//				Id<TaxibusLine> lineId = entry.getKey();
//				TaxibusVehicleRequestPath requestPath = this.currentRequestPathForLine.get(lineId);
//				
//				log.info("bus "+requestPath.vehicle.getId()+" has reached TwMax with "+requestPath.requests.size() + " requests planned");
//				fillPathWithDropOffsAndSchedule(requestPath, lineId);
//				this.currentRequestPathForLine.put(lineId, null);
//				resetTwMax.add(lineId);
//			}
//		}
//
//		for (Id<TaxibusLine> id : resetTwMax) {
//			this.currentTwMax.put(id, null);
//
//		}
//		//vehicle balancing tba
//
//	}
//
//	private VrpPathWithTravelData calculateVrpPath(Vehicle veh, TaxibusRequest req) {
//		LinkTimePair departure = optimConfig.scheduler.getImmediateDiversionOrEarliestIdleness(veh);
//		return departure == null ? //
//				null
//				: VrpPaths.calcAndCreatePath(departure.link, req.getFromLink(), departure.time, routerWithCache,
//						optimConfig.travelTime);
//	}
//
//	private VrpPathWithTravelData calculateFromPickupToPickup(VrpPathWithTravelData previous, TaxibusRequest current,
//			double time) {
//		return VrpPaths.calcAndCreatePath(previous.getToLink(), current.getFromLink(), time, routerWithCache,
//				optimConfig.travelTime);
//	}
//
//	private Tuple<VrpPathWithTravelData, TaxibusRequest> getNextDropoffSegment(Set<TaxibusRequest> allRequests,
//			double departureTime, Link departureLink) {
//
//		double bestTime = Double.MAX_VALUE;
//		Tuple<VrpPathWithTravelData, TaxibusRequest> bestSegment = null;
//		for (TaxibusRequest request : allRequests) {
//			VrpPathWithTravelData segment = VrpPaths.calcAndCreatePath(departureLink, request.getToLink(),
//					departureTime, routerWithCache, optimConfig.travelTime);
//			if (segment.getTravelTime() < bestTime) {
//				bestTime = segment.getTravelTime();
//				bestSegment = new Tuple<>(segment, request);
//
//			}
//		}
//
//		return bestSegment;
//	}
//
//}
