package org.matsim.modechoice.replanning;

import com.google.inject.Provider;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.modechoice.InformedModeChoiceConfigGroup;
import org.matsim.modechoice.search.TopKChoicesGenerator;

import javax.inject.Inject;

/**
 * Provider for {@link InformedModeChoicePlanStrategy}.
 */
public class InformedModeChoiceStrategyProvider implements Provider<PlanStrategy> {

	@Inject
	private InformedModeChoiceConfigGroup config;
	@Inject
	private ScoringParametersForPerson scoring;
	@Inject
	private TopKChoicesGenerator generator;

	@Override
	public PlanStrategy get() {
		return new InformedModeChoicePlanStrategy(config, scoring, generator);
	}
}
