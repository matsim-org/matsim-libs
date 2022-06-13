package org.matsim.modechoice.search;

import com.google.inject.Inject;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.modechoice.CandidateGenerator;
import org.matsim.modechoice.InformedModeChoiceConfigGroup;
import org.matsim.modechoice.PlanCandidate;

import java.util.Collection;
import java.util.Iterator;

/**
 * Generate top n choices for each possible mode option.
 */
public final class TopNChoicesGenerator implements CandidateGenerator {

	@Inject
	private InformedModeChoiceConfigGroup config;

	// TODO: estimators, options constrains, etc


	// TODO:

	public TopNChoicesGenerator() {
	}

	@Override
	public Collection<PlanCandidate> generate(Plan plan) {

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
