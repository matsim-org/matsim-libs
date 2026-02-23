package org.matsim.dsim.scoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Route;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

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
		}
		// we need something to track the
		else if (e instanceof PersonEntersVehicleEvent peve) {
		}


	}

	@Override
	public Route finishRoute() {
		return null;
	}

	@Override
	public Message toMessage() {
		return null;
	}

	static class Data {

		private double startTime;
		private double endTime;
		private double vehicleEnterTime;

		private Id<TransitStopFacility> startFacility;
		private Id<TransitStopFacility> endFacility;
		private Id<TransitLine> line;
		private Id<TransitRoute> route;
	}
}
