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
 * Generates simple back and force routes for two given stops and operation time, number of vehicles plying that line can be specified.
 * 
 * @author aneumann
 *
 */
@Deprecated
final class SimpleBackAndForthScheduleProvider implements PRouteProvider{
	
	private final static Logger log = Logger.getLogger(SimpleBackAndForthScheduleProvider.class);
	public final static String NAME = "SimpleBackAndForthScheduleProvider";
	
	private final String pIdentifier;
	private final Network net;
	private final TransitSchedule scheduleWithStopsOnly;
	private final RandomStopProvider randomStopProvider;
	private final String transportMode;
	private final double vehicleMaximumVelocity;
	
	public SimpleBackAndForthScheduleProvider(String pIdentifier, TransitSchedule scheduleWithStopsOnly, Network network, RandomStopProvider randomStopProvider, double vehicleMaximumVelocity, String transportMode) {
		this.pIdentifier = pIdentifier;
		this.net = network;
		this.scheduleWithStopsOnly = scheduleWithStopsOnly;
		this.randomStopProvider = randomStopProvider;
		this.transportMode = transportMode;
		this.vehicleMaximumVelocity = vehicleMaximumVelocity;
	}
	
	@Override
	public TransitLine createTransitLineFromOperatorPlan(Id<Operator> operatorId, PPlan plan){
		return this.createTransitLine(Id.create(operatorId, TransitLine.class), plan.getStartTime(), plan.getEndTime(), plan.getNVehicles(), plan.getStopsToBeServed(), Id.create(plan.getId(), TransitRoute.class));
	}

	private TransitLine createTransitLine(Id<TransitLine> pLineId, double startTime, double endTime, int numberOfVehicles, ArrayList<TransitStopFacility> stopsToBeServed, Id<TransitRoute> routeId){
		if (stopsToBeServed.size() != 2) {
			log.warn("This route provider can only handle as much as two stops. Please use a different route provider.");
			return null;
		}
		
		TransitStopFacility startStop = stopsToBeServed.get(0);
		TransitStopFacility endStop = stopsToBeServed.get(1);
		
		// initialize
		TransitLine line = this.scheduleWithStopsOnly.getFactory().createTransitLine(pLineId);			
		
		TransitRoute transitRoute_H = createRoute(Id.create(pLineId + "_" + routeId + "_H", TransitRoute.class), startStop, endStop);
		TransitRoute transitRoute_R = createRoute(Id.create(pLineId + "_" + routeId + "_R", TransitRoute.class), endStop, startStop);
		
		// register route
		line.addRoute(transitRoute_H);
		line.addRoute(transitRoute_R);
		
		// add departures
		int n = 0;
		int headway = (int) (transitRoute_H.getStop(endStop).getDepartureOffset() + transitRoute_R.getStop(startStop).getDepartureOffset()) / numberOfVehicles;
		// (headway = round trip time / number of vehicles)
		for (int i = 0; i < numberOfVehicles; i++) {
			for (double j = startTime + i * headway; j < endTime; ) {
				Departure departure = this.scheduleWithStopsOnly.getFactory().createDeparture(Id.create(n, Departure.class), j);
				departure.setVehicleId(Id.create(pLineId.toString() + "-" + i, Vehicle.class));
				transitRoute_H.addDeparture(departure);
				j += transitRoute_H.getStop(endStop).getDepartureOffset() + 1 *60;
				n++;

				departure = this.scheduleWithStopsOnly.getFactory().createDeparture(Id.create(n, Departure.class), j);
				departure.setVehicleId(Id.create(pLineId.toString() + "-" + i, Vehicle.class));
				transitRoute_R.addDeparture(departure);
				j += transitRoute_R.getStop(startStop).getDepartureOffset() + 1 *60;
				n++;
			}
		}		
		
		log.info("added " + n + " departures");		
		return line;
	}

	private TransitRoute createRoute(Id<TransitRoute> routeID, TransitStopFacility startStop, TransitStopFacility endStop){
		
		FreespeedTravelTimeAndDisutility tC = new FreespeedTravelTimeAndDisutility(-6.0, 0.0, 0.0);
		LeastCostPathCalculator routingAlgo = new Dijkstra(this.net, tC, tC);
		@SuppressWarnings("serial")
		Set<String> modes =  new HashSet<String>(){{
			// this is the networkmode and explicitly not the transportmode
				add(TransportMode.car);
			}};
		((Dijkstra) routingAlgo).setModeRestriction(modes);
		
		Node startNode = this.net.getLinks().get(startStop.getLinkId()).getToNode();
		Node endNode = this.net.getLinks().get(endStop.getLinkId()).getFromNode();
		
		int startTime = 0 * 3600;
		
		// get Route
		Path path = routingAlgo.calcLeastCostPath(startNode, endNode, startTime, null, null);
		NetworkRoute route = new LinkNetworkRouteImpl(startStop.getLinkId(), endStop.getLinkId());
		route.setLinkIds(startStop.getLinkId(), NetworkUtils.getLinkIds(path.links), endStop.getLinkId());		
		
		// get stops at Route
		List<TransitRouteStop> stops = new LinkedList<>();
							
		// first stop
		TransitRouteStop routeStop = this.scheduleWithStopsOnly.getFactory().createTransitRouteStop(startStop, startTime, startTime);
		stops.add(routeStop);
		
		// additional stops
		for (Link link : path.links) {
			startTime += link.getLength() / Math.min(this.vehicleMaximumVelocity, link.getFreespeed());
			if(this.scheduleWithStopsOnly.getFacilities().get(Id.create(this.pIdentifier + link.getId(), TransitStopFacility.class)) == null){
				continue;
			}
			routeStop = this.scheduleWithStopsOnly.getFactory().createTransitRouteStop(this.scheduleWithStopsOnly.getFacilities().get(Id.create(this.pIdentifier + link.getId(), TransitStopFacility.class)), startTime, startTime);
			stops.add(routeStop);
		}
		
		// last stop
		startTime += this.net.getLinks().get(endStop.getLinkId()).getLength() / Math.min(this.vehicleMaximumVelocity, this.net.getLinks().get(endStop.getLinkId()).getFreespeed());
		routeStop = this.scheduleWithStopsOnly.getFactory().createTransitRouteStop(endStop, startTime, startTime);
		stops.add(routeStop);
		
		// register departure
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