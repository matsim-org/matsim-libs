package org.matsim.dsim.scoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonContinuesInVehicleEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.mobsim.qsim.pt.PersonEntersPtVehicleEvent;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.pt.routes.DefaultTransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.List;

class BackpackTransitRoute implements BackpackRoute {

	private final Network network;
	private final TransitSchedule transitSchedule;
	private final List<Data> parts;

	/**
	 * Use this constructor when a new leg is started, and we want to
	 * start creating an experienced route.
	 *
	 * @param network         must be passed, so that we can compute distances.
	 * @param transitSchedule must be passed, so that we fetch transit relevant data such as facilities
	 */
	BackpackTransitRoute(Network network, TransitSchedule transitSchedule) {
		this(network, transitSchedule, new ArrayList<>());
	}

	/**
	 * Use this constructor when data of an incomplete route is received from another partition.
	 *
	 * @param network         the network is needed to compute distances once the route is finished
	 * @param parts           incomplete network route, which has been transferred from another partition.
	 * @param transitSchedule must be passed, so that we fetch transit relevant data such as facilities
	 */
	BackpackTransitRoute(Network network, TransitSchedule transitSchedule, List<Data> parts) {
		this.network = network;
		this.transitSchedule = transitSchedule;
		this.parts = parts;
	}

	@Override
	public void handleEvent(Event e) {

		if (e instanceof PersonDepartureEvent pde) {
			var data = new Data();
			data.startTime = pde.getTime();
			parts.add(data);
		} else if (e instanceof PersonArrivalEvent pae) {
			parts.getLast().endTime = pae.getTime();
		} else if (e instanceof PersonEntersPtVehicleEvent pepve) {
			var currentPart = parts.getLast();
			currentPart.boardingTime = pepve.getTime();
			currentPart.line = pepve.getTransitLine();
			currentPart.route = pepve.getTransitRoute();
			currentPart.vehicleId = pepve.getVehicleId();
		} else if (e instanceof VehicleArrivesAtFacilityEvent vaafe) {
			// this overrides the end facility at each stop. So, endFacility is more like
			// currentEndFacility
			parts.getLast().endFacility = vaafe.getFacilityId();
		} else if (e instanceof VehicleDepartsAtFacilityEvent vdafe) {
			if (parts.getLast().startFacility == null) {
				parts.getLast().startFacility = vdafe.getFacilityId();
			}
		} else if (e instanceof PersonContinuesInVehicleEvent pcive) {
			// finish current part
			parts.getLast().endTime = pcive.getTime();

			// start a new part
			var data = new Data();
			data.startTime = pcive.getTime();
			data.boardingTime = pcive.getTime();
			data.vehicleId = pcive.getVehicleId();
			data.line = pcive.getTransitLineId();
			data.route = pcive.getTransitRouteId();
			parts.add(data);
		}
	}

	@Override
	public Route finishRoute() {

		DefaultTransitPassengerRoute result = null;
		var endTime = parts.getLast().endTime;

		// we recursively create the transit route. The outermost element represents the overall route.
		// this is why we calculate the travel time always relative to the overall end time.
		// Similarly, we calculate the distance for each part by using the distance of all chained parts
		// plus the distance of the current part. (This is what calcRoute should do)
		while (!parts.isEmpty()) {
			var part = parts.removeLast();

			var start = transitSchedule.getFacilities().get(part.startFacility);
			var end = transitSchedule.getFacilities().get(part.endFacility);
			var transitLine = transitSchedule.getTransitLines().get(part.line);
			var transitRoute = transitLine.getRoutes().get(part.route);

			result = new DefaultTransitPassengerRoute(start, transitLine, transitRoute, end, result);
			var partDistance = RouteUtils.calcDistance(result, transitSchedule, network);
			var partTravelTime = endTime - part.startTime;

			result.setBoardingTime(part.boardingTime);
			result.setDistance(partDistance);
			result.setTravelTime(partTravelTime);
		}

		return result;
	}

	@Override
	public Message toMessage() {
		return new Msg(parts);
	}

	@Override
	public Id<Vehicle> getVehicleId() {
		return parts.isEmpty() ? null : parts.getLast().vehicleId;
	}

	static class Data {

		private double startTime;
		private double endTime;
		private double boardingTime;

		private Id<Vehicle> vehicleId;
		private Id<TransitStopFacility> startFacility;
		private Id<TransitStopFacility> endFacility;
		private Id<TransitLine> line;
		private Id<TransitRoute> route;
	}

	record Msg(List<Data> parts) implements Message {
	}
}
