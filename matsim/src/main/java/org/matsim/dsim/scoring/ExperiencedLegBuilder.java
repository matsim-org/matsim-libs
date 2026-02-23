package org.matsim.dsim.scoring;

import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.population.PopulationUtils;

public class ExperiencedLegBuilder {

	private final ExperiencedRouteBuilder routeBuilder;

	private String mode;
	private String routingMode;
	private double startTime;
	private double endTime;

	public ExperiencedLegBuilder(ExperiencedRouteBuilder routeBuilder) {
		this.routeBuilder = routeBuilder;
	}

	void handleEvent(Event e) {
		if (e instanceof PersonDepartureEvent pde) {
			startTime = pde.getTime();
			mode = pde.getLegMode();
			routingMode = pde.getRoutingMode();
		} else if (e instanceof PersonArrivalEvent pae) {
			endTime = pae.getTime();
		} else if (e instanceof PersonStuckEvent pse) {
			endTime = pse.getTime();
		}
		routeBuilder.handleEvent(e);
	}

	Leg finishLeg() {
		var result = PopulationUtils.createLeg(mode);
		result.setRoutingMode(routingMode);
		result.setDepartureTime(startTime);
		result.setTravelTime(endTime - startTime);
		result.setRoute(routeBuilder.finishRoute());
		return result;
	}
}
