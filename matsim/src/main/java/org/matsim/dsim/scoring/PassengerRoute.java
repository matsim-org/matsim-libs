package org.matsim.dsim.scoring;

import org.matsim.api.core.v01.population.Route;
import org.matsim.core.utils.misc.OptionalTime;

/**
 * Represents a passenger route with optional boarding time. This can be used to determine wait times of passengers.
 */
public interface PassengerRoute extends Route {

	OptionalTime getBoardingTime();
}
