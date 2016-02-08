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
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author johannes
 *
 */
public class CarLegSimEngine implements LegSimEngine {

	private final TravelTime travelTime;
	
	private final EventsManager events;
	
	private final Network network;

	private final Map<Person, Id<Vehicle>> vehicleIds;
	
	public CarLegSimEngine(Network network, TravelTime travelTime, EventsManager events) {
		this.network = network;
		this.events = events;
		this.travelTime = travelTime;
		vehicleIds = new HashMap<>();
	}
	
	@Override
	public double simulate(Person person, Leg leg, double departureTime, LinkedList<Event> eventList) {
		Id<Vehicle> vehicleId = vehicleIds.get(person);
		if(vehicleId == null) {
			vehicleId = Id.createVehicleId(person.getId());
			vehicleIds.put(person, vehicleId);
		}

		NetworkRoute route = (NetworkRoute) leg.getRoute();
		double tt = 0;

		events.processEvent(new VehicleEntersTrafficEvent(departureTime, person.getId(), route.getStartLinkId(),
				vehicleId, "car", 0));

		if (route.getStartLinkId() != route.getEndLinkId()) {
			Link link;
		
			List<Id<Link>> ids = route.getLinkIds();
			for (int i = 0; i < ids.size(); i++) {
				link = network.getLinks().get(ids.get(i));
				
				events.processEvent(new LinkEnterEvent(departureTime + tt, vehicleId, link.getId()));
//				eventList.add(new LinkEnterEvent(departureTime + tt, person.getId(), link.getId(), null));
				tt += travelTime.getLinkTravelTime(link, departureTime + tt, null, null);
				events.processEvent(new LinkLeaveEvent(departureTime + tt, vehicleId, link.getId()));
//				eventList.add(new LinkLeaveEvent(departureTime + tt, person.getId(), link.getId(), null));
				tt++;// 1 sec for each node
			}
			
			link = network.getLinks().get(route.getEndLinkId());
			events.processEvent(new LinkEnterEvent(departureTime + tt, vehicleId, link.getId()));
//			eventList.add(new LinkEnterEvent(departureTime + tt, person.getId(), link.getId(), null));
			tt += travelTime.getLinkTravelTime(link, departureTime + tt, null, null);
			events.processEvent(new LinkLeaveEvent(departureTime + tt, vehicleId, link.getId()));
//			eventList.add(new LinkLeaveEvent(departureTime + tt, person.getId(), link.getId(), null));
		}

		events.processEvent(new VehicleLeavesTrafficEvent(departureTime + tt, person.getId(), route.getEndLinkId(),
				vehicleId, "car", 0));

		return tt;
	}

}
