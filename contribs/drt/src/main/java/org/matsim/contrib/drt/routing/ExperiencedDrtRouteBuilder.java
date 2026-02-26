package org.matsim.contrib.drt.routing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerPickedUpEvent;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.dsim.scoring.ExperiencedRouteBuilder;
import org.matsim.dsim.scoring.PassengerRoute;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.List;

class ExperiencedDrtRouteBuilder implements ExperiencedRouteBuilder {
	private static final Logger log = LogManager.getLogger(ExperiencedDrtRouteBuilder.class);

	private final Data data;
	private final Network network;

	ExperiencedDrtRouteBuilder(Network network) {
		this(network, new Data());
	}

	ExperiencedDrtRouteBuilder(Network network, Data data) {
		this.data = data;
		this.network = network;
	}

	@Override
	public void handleEvent(Event e) {
		log.info(e);
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

	public static class ExperiencedDrtRoute implements PassengerRoute {

		private final Id<Link> startLink;
		private final Id<Link> endLink;
		private final OptionalTime travelTime;
		private final OptionalTime boardingTime;
		private final double distance;
		private final Id<Vehicle> vehicle;
		private final Id<Request> request;

		ExperiencedDrtRoute(Id<Link> startLink, Id<Link> endLink, Id<Vehicle> vehicle, Id<Request> request, double distance, OptionalTime travelTime, OptionalTime boardingTime) {
			this.startLink = startLink;
			this.endLink = endLink;
			this.vehicle = vehicle;
			this.distance = distance;
			this.travelTime = travelTime;
			this.boardingTime = boardingTime;
			this.request = request;
		}

		@Override
		public OptionalTime getBoardingTime() {
			return boardingTime;
		}

		@Override
		public double getDistance() {
			return distance;
		}

		@Override
		public void setDistance(double distance) {
			throw new UnsupportedOperationException();
		}

		@Override
		public OptionalTime getTravelTime() {
			return travelTime;
		}

		@Override
		public void setTravelTime(double travelTime) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setTravelTimeUndefined() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Id<Link> getStartLinkId() {
			return startLink;
		}

		@Override
		public Id<Link> getEndLinkId() {
			return endLink;
		}

		@Override
		public void setStartLinkId(Id<Link> linkId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setEndLinkId(Id<Link> linkId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getRouteDescription() {
			return "";
		}

		@Override
		public void setRouteDescription(String routeDescription) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getRouteType() {
			return "";
		}

		public Id<Vehicle> getVehicleId() {
			return vehicle;
		}

		public Id<Request> getRequestId() {
			return request;
		}

		@Override
		public Route clone() {
			throw new UnsupportedOperationException();
		}


	}
}
