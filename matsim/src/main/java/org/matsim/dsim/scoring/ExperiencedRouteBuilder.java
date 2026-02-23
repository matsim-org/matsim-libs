package org.matsim.dsim.scoring;

import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Route;

public interface ExperiencedRouteBuilder {

	void handleEvent(Event e);

	Route finishRoute();

	Message toMessage();
}
