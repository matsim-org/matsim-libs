package org.matsim.contrib.drt.routing;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.PassengerRoute;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.vehicles.Vehicle;

public class ExperiencedDrtRoute implements PassengerRoute {

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
