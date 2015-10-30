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

package playground.johannes.gsv.sim;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.util.TravelTime;

import java.util.LinkedList;
import java.util.List;

/**
 * @author johannes
 *
 */
public class CarLegSimEngine implements LegSimEngine {

	private final TravelTime travelTime;
	
	private final EventsManager events;
	
	private final Network network;
	
	public CarLegSimEngine(Network network, TravelTime travelTime, EventsManager events) {
		this.network = network;
		this.events = events;
		this.travelTime = travelTime;
	}
	
	@Override
	public double simulate(Person person, Leg leg, double departureTime, LinkedList<Event> eventList) {
		NetworkRoute route = (NetworkRoute) leg.getRoute();
		double tt = 0;
		
		if (route.getStartLinkId() != route.getEndLinkId()) {
			Link link;
		
			List<Id<Link>> ids = route.getLinkIds();
			for (int i = 0; i < ids.size(); i++) {
				link = network.getLinks().get(ids.get(i));
				
				events.processEvent(new LinkEnterEvent(departureTime + tt, person.getId(), link.getId(), null));
//				eventList.add(new LinkEnterEvent(departureTime + tt, person.getId(), link.getId(), null));
				tt += travelTime.getLinkTravelTime(link, departureTime + tt, null, null);
				events.processEvent(new LinkLeaveEvent(departureTime + tt, person.getId(), link.getId(), null));
//				eventList.add(new LinkLeaveEvent(departureTime + tt, person.getId(), link.getId(), null));
				tt++;// 1 sec for each node
			}
			
			link = network.getLinks().get(route.getEndLinkId());
			events.processEvent(new LinkEnterEvent(departureTime + tt, person.getId(), link.getId(), null));
//			eventList.add(new LinkEnterEvent(departureTime + tt, person.getId(), link.getId(), null));
			tt += travelTime.getLinkTravelTime(link, departureTime + tt, null, null);
			events.processEvent(new LinkLeaveEvent(departureTime + tt, person.getId(), link.getId(), null));
//			eventList.add(new LinkLeaveEvent(departureTime + tt, person.getId(), link.getId(), null));
		}

		return tt;
	}

}
