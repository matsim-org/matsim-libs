package org.matsim.contribs.discrete_mode_choice.components.utils;

import java.util.List;

import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.pt.routes.TransitPassengerRoute;

/**
 * This interface is used by the MATSim utility function estimator. It returns
 * the waiting time for an agent given a departure time and a route. To date,
 * the route itself does not contain this information. Therefore, the waiting
 * time needs to be inferred from the schedule.
 * 
 * @author sebhoerl
 */
public interface PTWaitingTimeEstimator {

	double estimateWaitingTime(double departureTime, TransitPassengerRoute route);

	double estimateWaitingTime(List<? extends PlanElement> elements);

}
