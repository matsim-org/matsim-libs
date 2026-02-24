package org.matsim.dsim.scoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scoring.EventsToLegs;
import org.matsim.vehicles.Vehicle;

class ExperiencedLegBuilder {

	private final ExperiencedRouteBuilder routeBuilder;
	private final Data data;

	ExperiencedLegBuilder(ExperiencedRouteBuilder routeBuilder) {
		this(new Data(), routeBuilder);
	}

	ExperiencedLegBuilder(Data data, ExperiencedRouteBuilder routeBuilder) {
		this.routeBuilder = routeBuilder;
		this.data = data;
	}

	void handleEvent(Event e) {
		if (e instanceof PersonDepartureEvent pde) {
			data.startTime = pde.getTime();
			data.mode = pde.getLegMode();
			data.routingMode = pde.getRoutingMode();
		} else if (e instanceof PersonArrivalEvent pae) {
			data.endTime = pae.getTime();
		} else if (e instanceof PersonStuckEvent pse) {
			data.endTime = pse.getTime();
		}
		routeBuilder.handleEvent(e);
	}

	Leg finishLeg() {
		var result = PopulationUtils.createLeg(data.mode);
		result.setRoutingMode(data.routingMode);
		result.setDepartureTime(data.startTime);
		result.setTravelTime(data.endTime - data.startTime);
		result.setRoute(routeBuilder.finishRoute());
		if (routeBuilder.getVehicleId() != null) {
			result.getAttributes().putAttribute(EventsToLegs.VEHICLE_ID_ATTRIBUTE_NAME, routeBuilder.getVehicleId());
		}
		return result;
	}

	Id<Vehicle> getCurrentVehicleId() {
		return routeBuilder.getVehicleId();
	}

	Msg toMessage() {
		return new Msg(data, routeBuilder.toMessage());
	}

	record Msg(Data data, Message routeBuilderData) implements Message {

		String getMode() {
			return data.mode;
		}
	}

	static class Data {
		private String mode;
		private String routingMode;
		private double startTime;
		private double endTime;
	}
}
