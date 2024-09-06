package org.matsim.contrib.bicycle;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.LinkSpeedCalculator;
import org.matsim.vehicles.Vehicle;

/**
 * interface so that this can be injected
 */
public interface BicycleLinkSpeedCalculator extends LinkSpeedCalculator {
	/**
	 * @deprecated -- I find it weird that we need a separate interface for something that it elsewhere in the code expressed by (..., ..., null,
	 * null).  Maybe should use "optional"??  kai, dec'22
	 */
	double getMaximumVelocityForLink(Link link, Vehicle vehicle);
}
