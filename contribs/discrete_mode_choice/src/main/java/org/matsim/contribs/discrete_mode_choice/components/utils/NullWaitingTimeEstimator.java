package org.matsim.contribs.discrete_mode_choice.components.utils;

import org.matsim.pt.routes.TransitPassengerRoute;

/**
 * Waiting time estimator which is used for the MATSim utility function
 * estimator if no TransitSchedule is available.
 * 
 * @author sebhoerl
 */
public class NullWaitingTimeEstimator implements PTWaitingTimeEstimator {
	@Override
	public double estimateWaitingTime(double agentDepartureTime, TransitPassengerRoute route) {
		return 0.0;
	}
}
