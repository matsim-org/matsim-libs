package ch.sbb.matsim.contrib.railsim.prototype;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.LinkSpeedCalculator;
import org.matsim.vehicles.Vehicle;

/**
 * interface so that this can be injected
 */
public interface RailsimLinkSpeedCalculator extends LinkSpeedCalculator {
	double getRailsimMaximumVelocity(Vehicle vehicle, Link link, double time);
}
