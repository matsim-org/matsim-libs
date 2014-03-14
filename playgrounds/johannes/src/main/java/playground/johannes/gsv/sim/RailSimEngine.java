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

import java.util.Queue;

import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * @author johannes
 *
 */
public class RailSimEngine implements LegSimEngine {
	
	private final Queue<Event> eventQueue;

	private final TransitSchedule schedule;
	
//	private final Network network;
	
	private final PreparedTransitSchedule preparedSchedule = new PreparedTransitSchedule(null);
	
	public RailSimEngine(Queue<Event> eventQueue, TransitSchedule schedule) {
		this.eventQueue = eventQueue;
		this.schedule = schedule;
	}
	
	/* (non-Javadoc)
	 * @see playground.johannes.gsv.sim.LegSimEngine#simulate(org.matsim.api.core.v01.population.Leg)
	 */
	@Override
	public double simulate(Person person, Leg leg, double departureTime) {
		ExperimentalTransitRoute route = (ExperimentalTransitRoute) leg.getRoute();
		TransitLine line = schedule.getTransitLines().get(route.getLineId());
		TransitRoute troute = line.getRoutes().get(route.getRouteId());
		
		TransitStopFacility accessStopFac = schedule.getFacilities().get(route.getAccessStopId());
		TransitStopFacility egressStopFac = schedule.getFacilities().get(route.getEgressStopId());
		
		TransitRouteStop accessStop = troute.getStop(accessStopFac);
		TransitRouteStop egressStop = troute.getStop(egressStopFac);
		
		double departure = preparedSchedule.getNextDepartureTime(troute, accessStop, departureTime);
		// throw event
		eventQueue.add(new TransitBoardEvent(departure, person, line, troute, accessStopFac));
		double arrival = egressStop.getArrivalOffset() + departure;
		eventQueue.add(new TransitAlightEvent(arrival, person, line, troute, egressStopFac));
//		NetworkRoute netRoute = troute.getRoute();
//		Link accessStationLink = network.getLinks().get(accessStopFac.getLinkId());
		return arrival - departure;
	}

}
