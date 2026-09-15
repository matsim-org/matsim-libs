package org.matsim.contrib.drt.routing;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PassengerRoute;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerPickedUpEvent;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.dsim.scoring.BackpackRoute;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.List;

class BackpackDrtRoute implements BackpackRoute {

	private final Data data;
	private final Network network;

	BackpackDrtRoute(Network network) {
		this(network, new Data());
	}

	BackpackDrtRoute(Network network, Data data) {
		this.data = data;
		this.network = network;
	}

	@Override
	public void handleEvent(Event e) {
		if (e instanceof PersonDepartureEvent pde) {
			data.startTime = pde.getTime();
			data.links.add(pde.getLinkId());
		} else if (e instanceof PersonEntersVehicleEvent peve) {
			data.vehicle = peve.getVehicleId();
		} else if (e instanceof PassengerPickedUpEvent ppup) {
			data.pickUpTime = ppup.getTime();
			data.request = ppup.getRequestId();
		} else if (e instanceof LinkEnterEvent lee) {
			data.links.add(lee.getLinkId());
		} else if (e instanceof PersonArrivalEvent pae) {
			data.endTime = pae.getTime();
		}
	}

	@Override
	public Route finishRoute() {

		var netRoute = RouteUtils.createNetworkRoute(data.links);
		var distance = RouteUtils.calcDistance(netRoute, 1., 1., network);
		var travelTime = OptionalTime.defined(data.endTime - data.startTime);
		var boardingTime = OptionalTime.defined(data.pickUpTime);

		return new ExperiencedDrtRoute(netRoute.getStartLinkId(), netRoute.getEndLinkId(), data.vehicle, data.request, distance, travelTime, boardingTime);
	}

	@Override
	public Message toMessage() {
		return data;
	}

	@Override
	public Id<Vehicle> getVehicleId() {
		return data.vehicle;
	}

	static class Data implements Message {
		double startTime;
		double pickUpTime;
		double endTime;
		private final List<Id<Link>> links = new ArrayList<>();
		Id<Request> request;
		Id<Vehicle> vehicle;
	}
}
