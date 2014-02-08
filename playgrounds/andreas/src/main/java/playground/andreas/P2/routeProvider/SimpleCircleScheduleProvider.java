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

package playground.andreas.P2.routeProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.andreas.P2.replanning.PPlan;


/**
 * Generates simple circle route for two given stops and operation time, number of vehicles plying that line can be specified.
 * 
 * @author aneumann
 *
 */
public class SimpleCircleScheduleProvider implements PRouteProvider {
	
	private final static Logger log = Logger.getLogger(SimpleCircleScheduleProvider.class);
	public final static String NAME = "SimpleCircleScheduleProvider";
	
	private String pIdentifier;
	private Network net;
	private TransitSchedule scheduleWithStopsOnly;
	private RandomStopProvider randomStopProvider;
	private String transportMode;
	private LeastCostPathCalculator routingAlgo;
	
	public SimpleCircleScheduleProvider(String pIdentifier, TransitSchedule scheduleWithStopsOnly, Network network, RandomStopProvider randomStopProvider, int iteration, final String transportMode) {
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
	}

	@Override
	public TransitLine createTransitLine(Id lineId, PPlan plan){
		return this.createTransitLine(lineId, plan.getStartTime(), plan.getEndTime(), plan.getNVehicles(), plan.getStopsToBeServed(), plan.getId());
	}
	
	private TransitLine createTransitLine(Id pLineId, double startTime, double endTime, int numberOfVehicles, ArrayList<TransitStopFacility> stopsToBeServed, Id routeId){
		if (stopsToBeServed.size() != 2) {
			log.warn("This route provider can only handle as much as to stops. Please use a different route provider.");
			return null;
		}
		
		// initialize
		TransitLine line = this.scheduleWithStopsOnly.getFactory().createTransitLine(pLineId);			
		routeId = new IdImpl(pLineId + "-" + routeId);
		TransitRoute transitRoute = createRoute(routeId, stopsToBeServed.get(0), stopsToBeServed.get(1), startTime);
		
		// register route
		line.addRoute(transitRoute);
		
		// add departures
		int n = 0;
		int headway = (int) (transitRoute.getStops().get(transitRoute.getStops().size() - 1).getDepartureOffset()) / numberOfVehicles;
		for (int i = 0; i < numberOfVehicles; i++) {
			for (double j = startTime + i * headway; j < endTime; ) {
				Departure departure = this.scheduleWithStopsOnly.getFactory().createDeparture(new IdImpl(n), j);
				departure.setVehicleId(new IdImpl(transitRoute.getId().toString() + "-" + i));
				transitRoute.addDeparture(departure);
				j += transitRoute.getStops().get(transitRoute.getStops().size() - 1).getDepartureOffset() + 1 *60;
				n++;
			}
		}		
		
		log.info("added " + n + " departures");		
		return line;
	}

	private TransitRoute createRoute(Id routeID, TransitStopFacility startStop, TransitStopFacility endStop, double startTime){
		
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
		
		List<Link> completeLinkList = new LinkedList<Link>();
		completeLinkList.addAll(forth.links);
		completeLinkList.add(this.net.getLinks().get(endStop.getLinkId()));
		completeLinkList.addAll(back.links);

		NetworkRoute route = new LinkNetworkRouteImpl(startStop.getLinkId(), startStop.getLinkId());
		route.setLinkIds(startStop.getLinkId(), NetworkUtils.getLinkIds(completeLinkList), startStop.getLinkId());		
		
		// get stops at Route
		List<TransitRouteStop> stops = new LinkedList<TransitRouteStop>();
		
		double runningTime = 0.0;
		
		// first stop
		TransitRouteStop routeStop = this.scheduleWithStopsOnly.getFactory().createTransitRouteStop(startStop, runningTime, runningTime);
		stops.add(routeStop);
		
		// additional stops
		for (Link link : completeLinkList) {
			runningTime += link.getLength() / link.getFreespeed();
			if(this.scheduleWithStopsOnly.getFacilities().get(new IdImpl(this.pIdentifier + link.getId())) == null){
				continue;
			}
			routeStop = this.scheduleWithStopsOnly.getFactory().createTransitRouteStop(this.scheduleWithStopsOnly.getFacilities().get(new IdImpl(this.pIdentifier + link.getId())), runningTime, runningTime);
			stops.add(routeStop);
		}
		
		// last stop
		runningTime += this.net.getLinks().get(startStop.getLinkId()).getLength() / this.net.getLinks().get(startStop.getLinkId()).getFreespeed();
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
	public TransitLine createEmptyLine(Id id) {
		return this.scheduleWithStopsOnly.getFactory().createTransitLine(id);
	}
	
	@Override
	public Collection<TransitStopFacility> getAllPStops() {
		return this.scheduleWithStopsOnly.getFacilities().values();
	}

}