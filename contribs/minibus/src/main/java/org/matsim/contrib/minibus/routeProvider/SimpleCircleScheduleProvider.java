/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.minibus.operator.Operator;
import org.matsim.contrib.minibus.operator.PPlan;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.Vehicle;

import java.util.*;


/**
 * Generates simple circle route for two given stops and operation time, number of vehicles plying that line can be specified.
 * 
 * @author aneumann
 *
 */
final class SimpleCircleScheduleProvider implements PRouteProvider {
	
	private final static Logger log = Logger.getLogger(SimpleCircleScheduleProvider.class);
	public final static String NAME = "SimpleCircleScheduleProvider";
	
	private final String pIdentifier;
	private final Network net;
	private final TransitSchedule scheduleWithStopsOnly;
	private final RandomStopProvider randomStopProvider;
	private final String transportMode;
	private final LeastCostPathCalculator routingAlgo;
	private final double vehicleMaximumVelocity;
	private final double driverRestTime;
	
	public SimpleCircleScheduleProvider(String pIdentifier, TransitSchedule scheduleWithStopsOnly, Network network, RandomStopProvider randomStopProvider, double vehicleMaximumVelocity, double driverRestTime, final String transportMode) {
		this.pIdentifier = pIdentifier;
		this.net = network;
		this.scheduleWithStopsOnly = scheduleWithStopsOnly;
		this.randomStopProvider = randomStopProvider;
		this.transportMode = transportMode;
		FreespeedTravelTimeAndDisutility tC = new FreespeedTravelTimeAndDisutility(-6.0, 0.0, 0.0);
		this.routingAlgo = new Dijkstra(this.net, tC, tC);
		@SuppressWarnings("serial")
		Set<String> modes =  new HashSet<String>(){{
			// this is the networkmode and explicitly not the transportmode
				add(TransportMode.car);
			}};
		((Dijkstra)this.routingAlgo).setModeRestriction(modes);
		
		this.vehicleMaximumVelocity = vehicleMaximumVelocity;
		this.driverRestTime = driverRestTime;
	}

	@Override
	public TransitLine createTransitLineFromOperatorPlan(Id<Operator> operatorId, PPlan plan){
		return this.createTransitLine(Id.create(operatorId, TransitLine.class), plan.getStartTime(), plan.getEndTime(), plan.getNVehicles(), plan.getStopsToBeServed(), Id.create(plan.getId(), TransitRoute.class));
	}
	
	private TransitLine createTransitLine(Id<TransitLine> pLineId, double startTime, double endTime, int numberOfVehicles, ArrayList<TransitStopFacility> stopsToBeServed, Id<TransitRoute> routeId){
		if (stopsToBeServed.size() != 2) {
			log.warn("This route provider can only handle as much as to stops. Please use a different route provider.");
			return null;
		}
		
		// initialize
		TransitLine line = this.scheduleWithStopsOnly.getFactory().createTransitLine(pLineId);			
		routeId = Id.create(pLineId + "-" + routeId, TransitRoute.class);
		TransitRoute transitRoute = createRoute(routeId, stopsToBeServed.get(0), stopsToBeServed.get(1), startTime);
		
		// register route
		line.addRoute(transitRoute);
		
		// add departures
		int n = 0;
		int headway = (int) (transitRoute.getStops().get(transitRoute.getStops().size() - 1).getDepartureOffset()) / numberOfVehicles;
		for (int i = 0; i < numberOfVehicles; i++) {
			for (double j = startTime + i * headway; j < endTime; ) {
				Departure departure = this.scheduleWithStopsOnly.getFactory().createDeparture(Id.create(n, Departure.class), j);
				departure.setVehicleId(Id.create(transitRoute.getId().toString() + "-" + i, Vehicle.class));
				transitRoute.addDeparture(departure);
				j += transitRoute.getStops().get(transitRoute.getStops().size() - 1).getDepartureOffset() + this.driverRestTime;
				n++;
			}
		}		
		
		log.info("added " + n + " departures");		
		return line;
	}

	private TransitRoute createRoute(Id<TransitRoute> routeID, TransitStopFacility startStop, TransitStopFacility endStop, double startTime){
		
//		FreespeedTravelTimeAndDisutility tC = new FreespeedTravelTimeAndDisutility(-6.0, 0.0, 0.0);
//		LeastCostPathCalculator routingAlgo = new Dijkstra(this.net, tC, tC);
		
		Node startNode = this.net.getLinks().get(startStop.getLinkId()).getToNode();
		Node endNode = this.net.getLinks().get(startStop.getLinkId()).getFromNode();
		Node intermediateEndNode = this.net.getLinks().get(endStop.getLinkId()).getFromNode();
		Node intermediateStartNode = this.net.getLinks().get(endStop.getLinkId()).getToNode();
		
		// get Route
//		Path
		Path forth = this.routingAlgo.calcLeastCostPath(startNode, intermediateEndNode, startTime, null, null);
		Path back = this.routingAlgo.calcLeastCostPath(intermediateStartNode, endNode, startTime + forth.travelTime, null, null);
		
		List<Link> completeLinkList = new LinkedList<>();
		completeLinkList.addAll(forth.links);
		completeLinkList.add(this.net.getLinks().get(endStop.getLinkId()));
		completeLinkList.addAll(back.links);

		NetworkRoute route = new LinkNetworkRouteImpl(startStop.getLinkId(), startStop.getLinkId());
		route.setLinkIds(startStop.getLinkId(), NetworkUtils.getLinkIds(completeLinkList), startStop.getLinkId());		
		
		// get stops at Route
		List<TransitRouteStop> stops = new LinkedList<>();
		
		double runningTime = 0.0;
		
		// first stop
		TransitRouteStop routeStop = this.scheduleWithStopsOnly.getFactory().createTransitRouteStop(startStop, runningTime, runningTime);
		stops.add(routeStop);
		
		// additional stops
		for (Link link : completeLinkList) {
			runningTime += link.getLength() / Math.min(this.vehicleMaximumVelocity, link.getFreespeed());
			if(this.scheduleWithStopsOnly.getFacilities().get(Id.create(this.pIdentifier + link.getId(), TransitStopFacility.class)) == null){
				continue;
			}
			routeStop = this.scheduleWithStopsOnly.getFactory().createTransitRouteStop(this.scheduleWithStopsOnly.getFacilities().get(Id.create(this.pIdentifier + link.getId(), TransitStopFacility.class)), runningTime, runningTime);
			stops.add(routeStop);
		}
		
		// last stop
		runningTime += this.net.getLinks().get(startStop.getLinkId()).getLength() / Math.min(this.vehicleMaximumVelocity, this.net.getLinks().get(startStop.getLinkId()).getFreespeed());
		routeStop = this.scheduleWithStopsOnly.getFactory().createTransitRouteStop(startStop, runningTime, runningTime);
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

}