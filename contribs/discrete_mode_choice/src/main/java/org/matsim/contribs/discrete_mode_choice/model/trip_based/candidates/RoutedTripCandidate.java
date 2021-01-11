package org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates;

import java.util.List;

import org.matsim.api.core.v01.population.PlanElement;

/**
 * Interface for a trip candidate holding a route.
 * 
 * @author sebhoerl
 */
public interface RoutedTripCandidate {
	List<? extends PlanElement> getRoutedPlanElements();
}
