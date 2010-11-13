package org.matsim.pt;

import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;

public interface UmlaufStueckI {

	TransitLine getLine();

	TransitRoute getRoute();

	Departure getDeparture();

	boolean isFahrt();

	NetworkRoute getCarRoute();

}
