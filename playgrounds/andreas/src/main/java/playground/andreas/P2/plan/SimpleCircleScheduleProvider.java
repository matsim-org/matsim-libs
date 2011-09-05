/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.andreas.P2.plan;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;


/**
 * Generates simple back and force routes for two given stops and operation time, number of vehicles plying that line can be specified.
 * 
 * @author aneumann
 *
 */
public class SimpleCircleScheduleProvider implements PRouteProvider {
	
	private final static Logger log = Logger.getLogger(SimpleCircleScheduleProvider.class);
	public final static String NAME = "SimpleCircleScheduleProvider";
	
	private NetworkImpl net;
	private TransitSchedule scheduleWithStopsOnly;
	
	public SimpleCircleScheduleProvider(TransitSchedule scheduleWithStopsOnly, NetworkImpl network, int iteration) {
		this.net = network;
		this.scheduleWithStopsOnly = scheduleWithStopsOnly;
	}

	public TransitLine createTransitLine(Id pLineId, double startTime, double endTime, int numberOfVehicles, TransitStopFacility startStop, TransitStopFacility endStop, Id routeId){
		// initialize
		TransitLine line = this.scheduleWithStopsOnly.getFactory().createTransitLine(pLineId);			
		routeId = new IdImpl(pLineId + "-" + routeId);
		TransitRoute transitRoute = createRoute(routeId, startStop, endStop, startTime);
		
		// register route
		line.addRoute(transitRoute);
		
		// add departures
		int n = 0;
		int headway = (int) (transitRoute.getStop(endStop).getDepartureOffset()) / numberOfVehicles;
		for (int i = 0; i < numberOfVehicles; i++) {
			for (double j = startTime + i * headway; j < endTime; ) {
				Departure departure = this.scheduleWithStopsOnly.getFactory().createDeparture(new IdImpl(n), j);
				departure.setVehicleId(new IdImpl(transitRoute.getId().toString() + "-" + i));
				transitRoute.addDeparture(departure);
				j += transitRoute.getStop(endStop).getDepartureOffset() + 1 *60;
				n++;
			}
		}		
		
		log.info("added " + n + " departures");		
		return line;
	}

	private TransitRoute createRoute(Id routeID, TransitStopFacility startStop, TransitStopFacility endStop, double startTime){
		
		FreespeedTravelTimeCost tC = new FreespeedTravelTimeCost(-6.0, 0.0, 0.0);
		LeastCostPathCalculator routingAlgo = new Dijkstra(this.net, tC, tC);
		
		Node startNode = this.net.getLinks().get(startStop.getLinkId()).getToNode();
		Node endNode = this.net.getLinks().get(startStop.getLinkId()).getFromNode();
		Node intermediateEndNode = this.net.getLinks().get(endStop.getLinkId()).getFromNode();
		Node intermediateStartNode = this.net.getLinks().get(endStop.getLinkId()).getToNode();
		
		// get Route
//		Path
		Path forth = routingAlgo.calcLeastCostPath(startNode, intermediateEndNode, startTime);
		Path back = routingAlgo.calcLeastCostPath(intermediateStartNode, endNode, startTime + forth.travelTime);
		
		List<Link> completeLinkList = new LinkedList<Link>();
		completeLinkList.addAll(forth.links);
		completeLinkList.add(this.net.getLinks().get(endStop.getLinkId()));
		completeLinkList.addAll(back.links);
		
		NetworkRoute route = (NetworkRoute) this.net.getFactory().createRoute(TransportMode.car, startStop.getLinkId(), startStop.getLinkId());
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
			if(this.scheduleWithStopsOnly.getFacilities().get(new IdImpl("p_" + link.getId())) == null){
				continue;
			}
			routeStop = this.scheduleWithStopsOnly.getFactory().createTransitRouteStop(this.scheduleWithStopsOnly.getFacilities().get(new IdImpl("p_" + link.getId())), runningTime, runningTime);
			stops.add(routeStop);
		}
		
		// last stop
		runningTime += this.net.getLinks().get(startStop.getLinkId()).getLength() / this.net.getLinks().get(startStop.getLinkId()).getFreespeed();
		routeStop = this.scheduleWithStopsOnly.getFactory().createTransitRouteStop(startStop, runningTime, runningTime);
		stops.add(routeStop);
		
		// register departure
		TransitRoute transitRoute = this.scheduleWithStopsOnly.getFactory().createTransitRoute(routeID, route, stops, "pt");
		
		return transitRoute;
	}

	public TransitStopFacility getRandomTransitStop(){
		int i = this.scheduleWithStopsOnly.getFacilities().size();
		for (TransitStopFacility stop : this.scheduleWithStopsOnly.getFacilities().values()) {
			if(MatsimRandom.getRandom().nextDouble() < 1.0 / i){
				return stop;
			}
			i--;
		}
		return null;
	}

	public TransitLine createEmptyLine(Id id) {
		return this.scheduleWithStopsOnly.getFactory().createTransitLine(id);
	}

}