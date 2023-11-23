/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.minibus.routeProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.minibus.hook.Operator;
import org.matsim.contrib.minibus.hook.PPlan;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

/**
 * Same as {@link ComplexCircleScheduleProvider}, but sets travel times according to the realized travel times of the last iteration.
 * 
 * @author aneumann
 *
 */
final class TimeAwareComplexCircleScheduleProvider implements PRouteProvider{

	private final static Logger log = LogManager.getLogger(TimeAwareComplexCircleScheduleProvider.class);
	public final static String NAME = "TimeAwareComplexCircleScheduleProvider";
	
	private final Network net;
	private final LeastCostPathCalculator routingAlgo;
	private final TransitSchedule scheduleWithStopsOnly;
	private final RandomStopProvider randomStopProvider;
	private final LinkedHashMap<Id<Link>, TransitStopFacility> linkId2StopFacilityMap;
	private final double vehicleMaximumVelocity;
	private final double planningSpeedFactor;
	private final double driverRestTime;
	
	private final TimeAwareComplexCircleScheduleProviderHandler handler;
	private final String transportMode;
	
	public TimeAwareComplexCircleScheduleProvider(TransitSchedule scheduleWithStopsOnly, Network network, RandomStopProvider randomStopProvider, double vehicleMaximumVelocity, double planningSpeedFactor, double driverRestTime, String pIdentifier, EventsManager eventsManager, final String transportMode) {
		this.net = network;
		this.scheduleWithStopsOnly = scheduleWithStopsOnly;
		FreespeedTravelTimeAndDisutility tC = new FreespeedTravelTimeAndDisutility(-6.0, 0.0, 0.0); // Here, it may make sense to use the variable cost parameters given in the config. Ihab/Daniel may'14
		this.routingAlgo = new DijkstraFactory().createPathCalculator(this.net, tC, tC);
		@SuppressWarnings("serial")
		Set<String> modes =  new HashSet<String>(){{
			// this is the networkmode and explicitly not the transportmode
			add(TransportMode.car);
//			add(TransportMode.pt);
			}};
		((Dijkstra)this.routingAlgo).setModeRestriction(modes);
		
		// register all stops by their corresponding link id
		this.linkId2StopFacilityMap = new LinkedHashMap<>();
		for (TransitStopFacility stop : this.scheduleWithStopsOnly.getFacilities().values()) {
			if (stop.getLinkId() == null) {
				log.warn("There is a potential paratransit stop without a corresponding link id. Shouldn't be possible. Check stop " + stop.getId());
			} else {
				this.linkId2StopFacilityMap.put(stop.getLinkId(), stop);
			}
		}
		
		this.randomStopProvider = randomStopProvider;
		this.vehicleMaximumVelocity = vehicleMaximumVelocity;
		this.planningSpeedFactor = planningSpeedFactor;
		this.driverRestTime = driverRestTime;
		
		this.handler = new TimeAwareComplexCircleScheduleProviderHandler(pIdentifier);
		eventsManager.addHandler(this.handler);
		this.transportMode = transportMode;
	}
	
	@Override
	public TransitLine createTransitLineFromOperatorPlan(Id<Operator> operatorId, PPlan plan){
		return this.createTransitLine(Id.create(operatorId, TransitLine.class), plan.getStartTime(), plan.getEndTime(), plan.getNVehicles(), plan.getStopsToBeServed(), Id.create(plan.getId(), TransitRoute.class));
	}
	
	private TransitLine createTransitLine(Id<TransitLine> pLineId, double startTime, double endTime, int numberOfVehicles, ArrayList<TransitStopFacility> stopsToBeServed, Id<TransitRoute> routeId){
		
		// initialize
		TransitLine line = this.scheduleWithStopsOnly.getFactory().createTransitLine(pLineId);			
		routeId = Id.create(pLineId + "-" + routeId, TransitRoute.class);
		TransitRoute transitRoute = createRoute(routeId, stopsToBeServed);
		
		// register route
		line.addRoute(transitRoute);
		
		// add departures
		int n = 0;
		/* After finishing one tour, vehicles wait the driver rest time and then start the next tour immediately.
		 * So, headway is a function of the number of vehicles and the time spent on one tour of the TransitRoute.
		 */
		int headway = (int) (transitRoute.getStops().get(transitRoute.getStops().size() - 1).getDepartureOffset().seconds() + this.driverRestTime) / numberOfVehicles;
		for (int i = 0; i < numberOfVehicles; i++) {
			for (double j = startTime + i * headway; j <= endTime; ) {
				Departure departure = this.scheduleWithStopsOnly.getFactory().createDeparture(Id.create(n, Departure.class), j);
				departure.setVehicleId(Id.create(transitRoute.getId().toString() + "-" + i, Vehicle.class));
				transitRoute.addDeparture(departure);
				j += transitRoute.getStops().get(transitRoute.getStops().size() - 1).getDepartureOffset().seconds() + this.driverRestTime;
				n++;
			}
		}		
		
//		log.info("added " + n + " departures");		
		return line;
	}
	
	private TransitRoute createRoute(Id<TransitRoute> routeID, ArrayList<TransitStopFacility> stopsToBeServed){
		
		ArrayList<TransitStopFacility> tempStopsToBeServed = new ArrayList<>();
		for (TransitStopFacility transitStopFacility : stopsToBeServed) {
			tempStopsToBeServed.add(transitStopFacility);
		}
		tempStopsToBeServed.add(stopsToBeServed.get(0));
		
		// create links - network route		
		Id<Link> startLinkId = null;
		Id<Link> lastLinkId = null;
		
		List<Link> links = new LinkedList<>();
		
		// for each stop
		for (TransitStopFacility stop : tempStopsToBeServed) {
			if(startLinkId == null){
				startLinkId = stop.getLinkId();
			}
			
			if(lastLinkId != null){
				links.add(this.net.getLinks().get(lastLinkId));
				Path path = this.routingAlgo.calcLeastCostPath(this.net.getLinks().get(lastLinkId).getToNode(), this.net.getLinks().get(stop.getLinkId()).getFromNode(), 0.0, null, null);

				for (Link link : path.links) {
					links.add(link);
				}
			}
			
			lastLinkId = stop.getLinkId();
		}

		links.remove(0);
		NetworkRoute route = RouteUtils.createLinkNetworkRouteImpl(startLinkId, lastLinkId);
		route.setLinkIds(startLinkId, NetworkUtils.getLinkIds(links), lastLinkId);

		// get stops at Route
		List<TransitRouteStop> stops = new LinkedList<>();
		double runningTime = 0.0;
		
		// first stop
		TransitRouteStop routeStop;
		routeStop = this.scheduleWithStopsOnly.getFactory().createTransitRouteStop(tempStopsToBeServed.get(0), runningTime, runningTime);
		stops.add(routeStop);
		
		// additional stops
		for (Link link : links) {
			runningTime += link.getLength() / (Math.min(this.vehicleMaximumVelocity, link.getFreespeed()) * this.planningSpeedFactor);
			if(this.linkId2StopFacilityMap.get(link.getId()) == null){
				continue;
			}
			
			// different from {@link ComplexCircleScheduleProvider}
			runningTime = modifyRunningTimeAccordingToTheLastIterationIfPossible(runningTime, this.handler.getOffsetForRouteAndStopNumber(routeID, stops.size()));
			// end
			
			routeStop = this.scheduleWithStopsOnly.getFactory().createTransitRouteStop(this.linkId2StopFacilityMap.get(link.getId()), runningTime, runningTime);
			stops.add(routeStop);
		}
		
		// last stop
		runningTime += this.net.getLinks().get(tempStopsToBeServed.get(0).getLinkId()).getLength() / (Math.min(this.vehicleMaximumVelocity, this.net.getLinks().get(tempStopsToBeServed.get(0).getLinkId()).getFreespeed()) * this.planningSpeedFactor);
		
		// different from {@link ComplexCircleScheduleProvider}
		runningTime = modifyRunningTimeAccordingToTheLastIterationIfPossible(runningTime, this.handler.getOffsetForRouteAndStopNumber(routeID, stops.size()));
		// end
		
		routeStop = this.scheduleWithStopsOnly.getFactory().createTransitRouteStop(tempStopsToBeServed.get(0), runningTime, runningTime);
		stops.add(routeStop);
		
		TransitRoute transitRoute = this.scheduleWithStopsOnly.getFactory().createTransitRoute(routeID, route, stops, this.transportMode);
		return transitRoute;
	}

	@Override
	public TransitStopFacility getRandomTransitStop(int currentIteration){
		return this.randomStopProvider.getRandomTransitStop(currentIteration);
	}
	
	@Override
	public TransitStopFacility drawRandomStopFromList(List<TransitStopFacility> choiceSet) {
		return this.randomStopProvider.drawRandomStopFromList(choiceSet);
	}

	@Override
	public TransitLine createEmptyLineFromOperator(Id<Operator> id) {
		return this.scheduleWithStopsOnly.getFactory().createTransitLine(Id.create(id, TransitLine.class));
	}

	@Override
	public Collection<TransitStopFacility> getAllPStops() {
		return this.scheduleWithStopsOnly.getFacilities().values();
	}
	
	private double modifyRunningTimeAccordingToTheLastIterationIfPossible(double runningTime, double offsetFromLastIteration){
		if (offsetFromLastIteration != -Double.MAX_VALUE) {
			runningTime = offsetFromLastIteration;
		}
		return runningTime;
	}
}
