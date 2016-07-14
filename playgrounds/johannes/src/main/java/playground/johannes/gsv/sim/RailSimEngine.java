/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes.gsv.sim;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * @author johannes
 *
 */
public class RailSimEngine implements LegSimEngine {
	
	private final Queue<Event> eventQueue;

	private final TransitSchedule schedule;
	
	private final Network network;
	
	private final PreparedTransitSchedule preparedSchedule = new PreparedTransitSchedule(null);
	
	public RailSimEngine(Queue<Event> eventQueue, TransitSchedule schedule, Network network) {
		this.eventQueue = eventQueue;
		this.schedule = schedule;
		this.network = network;
	}
	
	/* (non-Javadoc)
	 * @see playground.johannes.gsv.sim.LegSimEngine#simulate(org.matsim.api.core.v01.population.Leg)
	 */
	@Override
	public double simulate(Person person, Leg leg, double departureTime, LinkedList<Event> eventList) {
		ExperimentalTransitRoute route = (ExperimentalTransitRoute) leg.getRoute();
		TransitLine line = schedule.getTransitLines().get(route.getLineId());
		TransitRoute troute = line.getRoutes().get(route.getRouteId());
		
		TransitStopFacility accessStopFac = schedule.getFacilities().get(route.getAccessStopId());
		TransitStopFacility egressStopFac = schedule.getFacilities().get(route.getEgressStopId());
		
		TransitRouteStop accessStop = troute.getStop(accessStopFac);
		TransitRouteStop egressStop = troute.getStop(egressStopFac);
		
		double departure = preparedSchedule.getNextDepartureTime(troute, accessStop, departureTime);

		eventQueue.add(new TransitBoardEvent(departure, person, line, troute, accessStopFac));
		double arrival = egressStop.getArrivalOffset() + departure;
		double legTravelTime = arrival - departure;
		
		NetworkRoute netRoute = troute.getRoute();
		Link accessLink = network.getLinks().get(accessStopFac.getLinkId());
		Node accessNode = accessLink.getToNode();
		
		Link egressLink = network.getLinks().get(egressStopFac.getLinkId());
		Node egressNode = egressLink.getFromNode();
		
		int startIdx = -1;
		List<Id> ids = new ArrayList<Id>(netRoute.getLinkIds().size() + 2);
		ids.add(netRoute.getStartLinkId());
		ids.addAll(netRoute.getLinkIds());
		ids.add(netRoute.getEndLinkId());
		
		for(int i = 0; i < ids.size(); i++) {
			Link link = network.getLinks().get(ids.get(i));
			if(link.getFromNode().equals(accessNode)) {
				startIdx = i;
				break;
			}
		}
		
		if(startIdx < 0)
			throw new RuntimeException("Access stop not found in transit route.");
		
		boolean found = false;
		List<Link> links = new ArrayList<Link>(ids.size());
		for(int i = startIdx; i < ids.size(); i++) {
			Link link = network.getLinks().get(ids.get(i));
			links.add(link);
			
			if(link.getToNode().equals(egressNode)) {
				found = true;
				break;
			}
		}
		
		if(!found)
			throw new RuntimeException("Egress node not found.");
		
		double time = departure;
		
		double linkTTime = Math.ceil(legTravelTime / (double)links.size());
		for(Link link : links) {
			eventQueue.add(new LinkEnterEvent(time, null, link.getId()));
			time += linkTTime;
			time = Math.min(time, arrival);
			eventQueue.add(new LinkLeaveEvent(time, null, link.getId()));
		}
		
		eventQueue.add(new TransitAlightEvent(arrival, person, line, troute, egressStopFac));
		
		return legTravelTime;
	}

}
