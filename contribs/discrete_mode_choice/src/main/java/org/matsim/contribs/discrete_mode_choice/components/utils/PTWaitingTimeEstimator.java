package org.matsim.contribs.discrete_mode_choice.components.utils;

import java.util.List;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
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

	default double estimateWaitingTime(List<? extends PlanElement> elements) {
		double totalWaitingTime = 0.0;

		for (PlanElement element : elements) {
			if (element instanceof Leg leg && leg.getMode().equals(TransportMode.pt)) {
				TransitPassengerRoute route = (TransitPassengerRoute) leg.getRoute();
				totalWaitingTime += this.estimateWaitingTime(leg.getDepartureTime().seconds(), route);
			}
		}

		return totalWaitingTime;
	}
}
