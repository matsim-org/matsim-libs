package org.matsim.contrib.bicycle;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.LinkSpeedCalculator;
import org.matsim.vehicles.Vehicle;

/**
 * interface so that this can be injected
 */
public interface BicycleLinkSpeedCalculator extends LinkSpeedCalculator {
	double getMaximumVelocityForLink(Link link, Vehicle vehicle);
}
