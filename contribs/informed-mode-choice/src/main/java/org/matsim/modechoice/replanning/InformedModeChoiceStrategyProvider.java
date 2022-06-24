package org.matsim.modechoice.replanning;

import com.google.inject.Provider;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.modechoice.search.TopKChoicesGenerator;

import javax.inject.Inject;

/**
 * Provider for {@link InformedModeChoicePlanStrategy}.
 */
public class InformedModeChoiceStrategyProvider implements Provider<PlanStrategy> {

	@Inject
	private TopKChoicesGenerator generator;

	@Override
	public PlanStrategy get() {
		return new InformedModeChoicePlanStrategy(generator);
	}
}
