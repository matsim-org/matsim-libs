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

package playground.andreas.P2.routeProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
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
 * Same as {@link ComplexCircleScheduleProvider}, but sets travel times according to the realized travel times of the last iteration.
 * 
 * @author aneumann
 *
 */
public class TimeAwareComplexCircleScheduleProvider implements PRouteProvider{

	private final static Logger log = Logger.getLogger(TimeAwareComplexCircleScheduleProvider.class);
	public final static String NAME = "TimeAwareComplexCircleScheduleProvider";
	
	private Network net;
	private LeastCostPathCalculator routingAlgo;
	private TransitSchedule scheduleWithStopsOnly;
	private RandomStopProvider randomStopProvider;
	private LinkedHashMap<Id, TransitStopFacility> linkId2StopFacilityMap;
	private double vehicleMaximumVelocity;
	private double planningSpeedFactor;
	
	private TimeAwareComplexCircleScheduleProviderHandler handler;
	private String transportMode;
	
	public TimeAwareComplexCircleScheduleProvider(TransitSchedule scheduleWithStopsOnly, Network network, RandomStopProvider randomStopProvider, int iteration,	double vehicleMaximumVelocity, double planningSpeedFactor, String pIdentifier, EventsManager eventsManager, final String transportMode) {
		this.net = network;
		this.scheduleWithStopsOnly = scheduleWithStopsOnly;
		FreespeedTravelTimeAndDisutility tC = new FreespeedTravelTimeAndDisutility(-6.0, 0.0, 0.0);
		this.routingAlgo = new Dijkstra(this.net, tC, tC);
		@SuppressWarnings("serial")
		Set<String> modes =  new HashSet<String>(){{
			// this is the networkmode and explicitly not the transportmode
			add(TransportMode.car);
//			add(TransportMode.pt);
			}};
		((Dijkstra)this.routingAlgo).setModeRestriction(modes);
		
		// register all stops by their corresponding link id
		this.linkId2StopFacilityMap = new LinkedHashMap<Id, TransitStopFacility>();
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
		
		this.handler = new TimeAwareComplexCircleScheduleProviderHandler(pIdentifier);
		eventsManager.addHandler(this.handler);
		this.transportMode = transportMode;
	}
	
	@Override
	public TransitLine createTransitLine(Id lineId, PPlan plan){
		return this.createTransitLine(lineId, plan.getStartTime(), plan.getEndTime(), plan.getNVehicles(), plan.getStopsToBeServed(), plan.getId());
	}
	
	private TransitLine createTransitLine(Id pLineId, double startTime, double endTime, int numberOfVehicles, ArrayList<TransitStopFacility> stopsToBeServed, Id routeId){
		
		// initialize
		TransitLine line = this.scheduleWithStopsOnly.getFactory().createTransitLine(pLineId);			
		routeId = new IdImpl(pLineId + "-" + routeId);
		TransitRoute transitRoute = createRoute(routeId, stopsToBeServed, startTime);
		
		// register route
		line.addRoute(transitRoute);
		
		// add departures
		int n = 0;
		int headway = (int) (transitRoute.getStops().get(transitRoute.getStops().size() - 1).getDepartureOffset()) / numberOfVehicles;
		for (int i = 0; i < numberOfVehicles; i++) {
			for (double j = startTime + i * headway; j <= endTime; ) {
				Departure departure = this.scheduleWithStopsOnly.getFactory().createDeparture(new IdImpl(n), j);
				departure.setVehicleId(new IdImpl(transitRoute.getId().toString() + "-" + i));
				transitRoute.addDeparture(departure);
				j += transitRoute.getStops().get(transitRoute.getStops().size() - 1).getDepartureOffset() + 1 *60;
				n++;
			}
		}		
		
//		log.info("added " + n + " departures");		
		return line;
	}
	
	private TransitRoute createRoute(Id routeID, ArrayList<TransitStopFacility> stopsToBeServed, double startTime){
		
		ArrayList<TransitStopFacility> tempStopsToBeServed = new ArrayList<TransitStopFacility>();
		for (TransitStopFacility transitStopFacility : stopsToBeServed) {
			tempStopsToBeServed.add(transitStopFacility);
		}
		tempStopsToBeServed.add(stopsToBeServed.get(0));
		
		// create links - network route		
		Id startLinkId = null;
		Id lastLinkId = null;
		
		List<Link> links = new LinkedList<Link>();				
		
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
		LinkNetworkRouteImpl route = new LinkNetworkRouteImpl(startLinkId, lastLinkId);
		route.setLinkIds(startLinkId, NetworkUtils.getLinkIds(links), lastLinkId);

		// get stops at Route
		List<TransitRouteStop> stops = new LinkedList<TransitRouteStop>();
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
	public TransitLine createEmptyLine(Id id) {
		return this.scheduleWithStopsOnly.getFactory().createTransitLine(id);
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
