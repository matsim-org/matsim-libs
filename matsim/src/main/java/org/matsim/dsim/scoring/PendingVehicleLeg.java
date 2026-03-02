package org.matsim.dsim.scoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.vehicles.Vehicle;

class PendingVehicleLeg {

	private final double enterVehicleTime;
	private final PendingLeg basicLeg;

	private Id<Vehicle> vehicleId;
	private double relativePositionOnDepartureLink;
	private double relativePositionOnLastArrivalLink;

	PendingVehicleLeg(double enterVehicleTime, Id<Vehicle> vehicleId, PendingLeg basicLeg) {
		this.enterVehicleTime = enterVehicleTime;
		this.vehicleId = vehicleId;
		this.basicLeg = basicLeg;
	}

	void relativePositionOnDepartureLink(double pos) {
		relativePositionOnDepartureLink = pos;
	}

	double relativePositionOnDepartureLink() {
		return relativePositionOnDepartureLink;
	}

	void relativePositionOnArrivalLink(double pos) {
		relativePositionOnLastArrivalLink = pos;
	}

	double relativePositionOnArrivalLink() {
		return relativePositionOnLastArrivalLink;
	}

	void vehicleId(Id<Vehicle> vehicleId) {
		this.vehicleId = vehicleId;
	}

	Id<Vehicle> vehicleId() {
		return vehicleId;
	}

	double enterVehicleTime() {
		return enterVehicleTime;
	}

	void addLink(Id<Link> id) {
		basicLeg.addLink(id);
	}

	NetworkRoute route() {
		var links = basicLeg.routeLinks();
		return RouteUtils.createNetworkRoute(links);
	}
}
