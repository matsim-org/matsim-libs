package org.matsim.dsim.scoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.vehicles.Vehicle;

class BackpackGenericRoute implements BackpackRoute, Message {

	private Id<Link> startLink;
	private Id<Link> endLink;
	private double startTime;
	private double endTime;
	private double distance;


	@Override
	public void handleEvent(Event e) {
		if (e instanceof PersonDepartureEvent pde) {
			startTime = pde.getTime();
			startLink = pde.getLinkId();
		} else if (e instanceof PersonArrivalEvent pae) {
			endTime = pae.getTime();
			endLink = pae.getLinkId();
		} else if (e instanceof TeleportationArrivalEvent tae) {
			distance = tae.getDistance();
		}
	}

	@Override
	public Route finishRoute() {
		var result = RouteUtils.createGenericRouteImpl(startLink, endLink);
		result.setDistance(distance);
		result.setTravelTime(endTime - startTime);
		return result;
	}

	@Override
	public Message toMessage() {
		// we can send ourselves as a message, as we only have plain data types.
		return this;
	}

	@Override
	public Id<Vehicle> getVehicleId() {
		return null;
	}
}
