package org.matsim.core.router;

import org.matsim.api.core.v01.population.Person;
import org.matsim.facilities.Facility;
import org.matsim.utils.objectattributes.attributable.Attributable;

/**
 * Defines the characteristics of a route to be found, independent of transport
 * mode
 */
public interface RoutingRequest extends Attributable {
	Facility getFromFacility();

	Facility getToFacility();

	double getDepartureTime();

	Person getPerson();
}
