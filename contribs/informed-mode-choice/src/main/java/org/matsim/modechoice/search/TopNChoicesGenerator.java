package org.matsim.modechoice.search;

import com.google.inject.Inject;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.modechoice.*;
import org.matsim.modechoice.estimators.LegEstimator;
import org.matsim.modechoice.estimators.PlanBasedLegEstimator;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Generate top n choices for each possible mode option.
 */
public final class TopNChoicesGenerator implements CandidateGenerator {

	@Inject
	private InformedModeChoiceConfigGroup config;

	@Inject
	private Map<String, LegEstimator<?>> estimators;

	@Inject
	private Map<String, PlanBasedLegEstimator<?>> planEstimators;

	@Inject
	private Map<String, ModeOptions<?>> options;

	@Inject
	private ScoringParametersForPerson params;

	@Inject
	private EstimateRouter router;

	// TODO: estimators, options constrains, etc


	// TODO:

	public TopNChoicesGenerator() {
	}

	/**
	 * Lookup entry from the map.
	 */
	private <T> T lookup(Map<String, T> map, String mode) {
		T entry = map.get(mode);
		if (entry == null)
			entry = map.get(InformedModeChoiceModule.ANY_MODE);

		return entry;
	}

	@Override
	public Collection<PlanCandidate> generate(Plan plan) {

		EstimatorContext context = new EstimatorContext(plan.getPerson(), params.getScoringParameters(plan.getPerson()));

		for (String mode : config.getModes()) {

			ModeOptions<?> t = lookup(options, mode);

			LegEstimator<?> est = lookup(estimators, mode);

			List<?> options = t.get(plan.getPerson());

		}





		// TODO: generate all mode options

		// TODO trip route per mode (if usable)
		// utility estimate per mode and plan option


		// TODO: always generate the plan model?
		// but only plan based estimate has access to it

		// collect all legs from the router for possible modes
		// generate all options before hand and only leave them out during search

		return null;
	}
}
