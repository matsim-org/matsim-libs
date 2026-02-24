package org.matsim.dsim.scoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.events.*;
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

public class ExperiencedTransitRouteBuilder implements ExperiencedRouteBuilder {

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
	ExperiencedTransitRouteBuilder(Network network, TransitSchedule transitSchedule) {
		this(network, transitSchedule, new ArrayList<>());
	}

	/**
	 * Use this constructor when data of an incomplete route is received from another partition.
	 *
	 * @param network         the network is needed to compute distances once the route is finished
	 * @param parts           incomplete network route, which has been transferred from another partition.
	 * @param transitSchedule must be passed, so that we fetch transit relevant data such as facilities
	 */
	ExperiencedTransitRouteBuilder(Network network, TransitSchedule transitSchedule, List<Data> parts) {
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
			currentPart.vehicleEnterTime = pepve.getTime();
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
			data.vehicleEnterTime = pcive.getTime();
			data.vehicleId = pcive.getVehicleId();
			data.line = pcive.getTransitLineId();
			data.route = pcive.getTransitRouteId();
			parts.add(data);
		} else if (e instanceof PersonStuckEvent pse) {
			parts.getLast().endTime = pse.getTime();
		}
	}

	@Override
	public Route finishRoute() {

		DefaultTransitPassengerRoute result = null;
		// the following attributes are over all parts of the route. Retreive them before we
		// remove the individual parts of the route. Yet, they have to be set on the head part
		// of the route.
		var firstPart = parts.getFirst();
		var enterTime = firstPart.vehicleEnterTime;
		var startTime = firstPart.startTime;
		var endTime = parts.getLast().endTime;

		while (!parts.isEmpty()) {
			var part = parts.removeLast();

			var start = transitSchedule.getFacilities().get(part.startFacility);
			var end = transitSchedule.getFacilities().get(part.endFacility);
			var transitLine = transitSchedule.getTransitLines().get(part.line);
			var transitRoute = transitLine.getRoutes().get(part.route);

			result = new DefaultTransitPassengerRoute(start, transitLine, transitRoute, end, result);
		}

		assert result != null;

		// set the following
		result.setBoardingTime(enterTime);
		result.setTravelTime(endTime - startTime);
		result.setDistance(RouteUtils.calcDistance(result, transitSchedule, network));

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
		private double vehicleEnterTime;

		private Id<Vehicle> vehicleId;
		private Id<TransitStopFacility> startFacility;
		private Id<TransitStopFacility> endFacility;
		private Id<TransitLine> line;
		private Id<TransitRoute> route;
	}

	record Msg(List<Data> parts) implements Message {
	}
}
