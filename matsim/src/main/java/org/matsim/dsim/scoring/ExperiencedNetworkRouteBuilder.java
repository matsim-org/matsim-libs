package org.matsim.dsim.scoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.List;

public class ExperiencedNetworkRouteBuilder implements ExperiencedRouteBuilder {

	private final Network network;
	private final Data data;

	/**
	 * Use this constructor when a new leg is started and we want to
	 * start creating an experienced route.
	 *
	 * @param network must be passed, so that we can compute distances.
	 */
	ExperiencedNetworkRouteBuilder(Network network) {
		this(network, new Data());
	}

	/**
	 * Use this constructor when data of an incomplete route is received from another partition.
	 *
	 * @param network the network is needed to compute distances once the route is finished
	 * @param data    incomplete network route, which has been transferred from another partition.
	 */
	ExperiencedNetworkRouteBuilder(Network network, Data data) {
		this.network = network;
		this.data = data;
	}

	@Override
	public void handleEvent(Event e) {
		if (e instanceof PersonDepartureEvent pde) {
			data.startTime = pde.getTime();
			data.links.add(pde.getLinkId());
		} else if (e instanceof PersonArrivalEvent pae) {
			data.endTime = pae.getTime(); // don't store the arrival link as it should have been collected by link enter event
		} else if (e instanceof PersonEntersVehicleEvent peve) {
			data.vehicleId = peve.getVehicleId();
		} else if (e instanceof VehicleEntersTrafficEvent vete) {
			data.relativePositionOnDepartureLink = vete.getRelativePositionOnLink();
		} else if (e instanceof VehicleLeavesTrafficEvent vlte) {
			data.relativePostionOnArrivalLink = vlte.getRelativePositionOnLink();
		} else if (e instanceof LinkEnterEvent lee) {
			data.links.add(lee.getLinkId());
		}
	}

	@Override
	public Route finishRoute() {

		var result = RouteUtils.createNetworkRoute(data.links);
		var travelledDistance = RouteUtils.calcDistance(
			result, data.relativePositionOnDepartureLink, data.relativePostionOnArrivalLink, network
		);
		result.setDistance(travelledDistance);
		result.setTravelTime(data.endTime - data.startTime);
		result.setVehicleId(data.vehicleId);
		return result;
	}

	@Override
	public Message toMessage() {
		return data;
	}

	@Override
	public Id<Vehicle> getVehicleId() {
		return data.vehicleId;
	}

	/**
	 * This is a class and not a record, because things must be mutable.
	 */
	static class Data implements Message {
		private final List<Id<Link>> links = new ArrayList<>();
		private double relativePositionOnDepartureLink;
		private double relativePostionOnArrivalLink;
		private double startTime;
		private double endTime;
		private Id<Vehicle> vehicleId;
	}
}
