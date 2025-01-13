package org.matsim.modechoice.replanning;

import com.google.inject.Provider;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.modechoice.InformedModeChoiceConfigGroup;
import org.matsim.modechoice.ModeChoiceWeightScheduler;

import jakarta.inject.Inject;

/**
 * Creates mnl selector with current set weights.
 */
public class MultinomialLogitSelectorProvider implements Provider<PlanSelector> {

	@Inject
	private ModeChoiceWeightScheduler weights;

	@Inject
	private Config config;

	@Override
	public PlanSelector get() {

		InformedModeChoiceConfigGroup c = ConfigUtils.addOrGetModule(config, InformedModeChoiceConfigGroup.class);
		if (c.isNormalizeUtility()) {
			return new NormalizedMultinomialLogitSelector(weights.getInvBeta(), MatsimRandom.getLocalInstance());
		}

		return new MultinomialLogitSelector(weights.getInvBeta(), MatsimRandom.getLocalInstance());
	}
}
