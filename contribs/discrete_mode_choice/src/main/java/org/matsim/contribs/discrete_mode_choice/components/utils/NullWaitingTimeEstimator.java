package org.matsim.contribs.discrete_mode_choice.components.utils;

import java.util.List;

import org.matsim.api.core.v01.population.PlanElement;
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

	@Override
	public double estimateWaitingTime(List<? extends PlanElement> elements) {
		return 0.0;
	}

}
