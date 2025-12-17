package org.matsim.dsim.scoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.RouteUtils;

import java.util.ArrayList;
import java.util.List;

public class PendingLeg {

	private final String legMode;
	private final String routingMode;
	private final double departureTime;
	private final List<Id<Link>> route = new ArrayList<>();

	private double arrivalTime;
	private double travelDistance;

	public PendingLeg(String legMode, String routingMode, double departureTime) {
		this.legMode = legMode;
		this.routingMode = routingMode;
		this.departureTime = departureTime;
	}

	void addLink(Id<Link> link) {
		route.add(link);
	}

	void arrivalTime(double travelTime) {
		this.arrivalTime = travelTime;
	}

	void travelDistance(double travelDistance) {
		this.travelDistance = travelDistance;
	}

	double travelDistance() {
		return travelDistance;
	}

	double travelTime() {
		return arrivalTime - departureTime;
	}

	double departureTime() {
		return departureTime;
	}

	String legMode() {
		return legMode;
	}

	String routingMode() {
		return routingMode;
	}

	List<Id<Link>> routeLinks() {
		return route;
	}

	Route route() {
		if (this.route.size() < 2)
			throw new IllegalStateException("Route must contain at least two links");

		return RouteUtils.createGenericRouteImpl(route.getFirst(), route.getLast());

	}
}
