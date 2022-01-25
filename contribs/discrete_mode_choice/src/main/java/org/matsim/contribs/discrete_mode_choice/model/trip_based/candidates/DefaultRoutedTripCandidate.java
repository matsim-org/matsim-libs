package org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates;

import java.util.List;

import org.matsim.api.core.v01.population.PlanElement;

/**
 * A default implementation for a trip candidate with a route.
 * 
 * @author sebhoerl
 */
public class DefaultRoutedTripCandidate extends DefaultTripCandidate implements RoutedTripCandidate {
	private final List<? extends PlanElement> routedPlanElements;

	public DefaultRoutedTripCandidate(double utility, String mode, List<? extends PlanElement> routedPlanElements,
			double duration) {
		super(utility, mode, duration);
		this.routedPlanElements = routedPlanElements;
	}

	@Override
	public List<? extends PlanElement> getRoutedPlanElements() {
		return routedPlanElements;
	}
}
