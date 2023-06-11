package org.matsim.modechoice.replanning;

import com.google.inject.Provider;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.modechoice.ModeChoiceWeightScheduler;

import jakarta.inject.Inject;

/**
 * Creates mnl selector with current set weights.
 */
public class MultinomialLogitSelectorProvider implements Provider<PlanSelector> {

	@Inject
	private ModeChoiceWeightScheduler weights;

	@Override
	public PlanSelector get() {
		return new MultinomialLogitSelector(weights.getInvBeta(), MatsimRandom.getLocalInstance());
	}
}
