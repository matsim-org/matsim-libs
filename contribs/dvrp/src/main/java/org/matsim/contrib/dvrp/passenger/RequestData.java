package org.matsim.contrib.dvrp.passenger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;

/**
 * A data container that stores initially provided request information, required
 * for a request retry.
 * 
 * @author Steffen Axer
 *
 */

public class RequestData {

	MobsimPassengerAgent passenger;
	Route route;
	Id<Request> requestId;
	Id<Link> fromLink;
	Id<Link> toLink;
	Double desiredDepartureTime;

	public RequestData(Id<Request> requestId, MobsimPassengerAgent passenger, Route route, Id<Link> fromLink,
			Id<Link> toLink, Double desiredDepartureTime) {
		this.requestId = requestId;
		this.passenger = passenger;
		this.route = route;
		this.fromLink = fromLink;
		this.toLink = toLink;
		this.desiredDepartureTime = desiredDepartureTime;
	}

	public Id<Request> getRequestId() {
		return requestId;
	}

	public void setRequestId(Id<Request> requestId) {
		this.requestId = requestId;
	}

	public MobsimPassengerAgent getPassenger() {
		return passenger;
	}

	public void setPassenger(MobsimPassengerAgent passenger) {
		this.passenger = passenger;
	}

	public Route getRoute() {
		return route;
	}

	public void setRoute(Route route) {
		this.route = route;
	}

	public Id<Link> getFromLink() {
		return fromLink;
	}

	public void setFromLink(Id<Link> fromLink) {
		this.fromLink = fromLink;
	}

	public Id<Link> getToLink() {
		return toLink;
	}

	public void setToLink(Id<Link> toLink) {
		this.toLink = toLink;
	}

	public Double getDesiredDepartureTime() {
		return desiredDepartureTime;
	}

	public void setDesiredDepartureTime(Double desiredDepartureTime) {
		this.desiredDepartureTime = desiredDepartureTime;
	}

}