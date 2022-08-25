package org.matsim.core.replanning.choosers;

import org.matsim.api.core.v01.population.BasicPlan;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.ReplanningContext;

/**
 * Default strategy chooser based on the configured weights.
 */
public class WeightedStrategyChooser<PL extends BasicPlan, AG extends HasPlansAndId<? extends BasicPlan, AG>> implements StrategyChooser<PL, AG> {

	@Override
	public GenericPlanStrategy<PL, AG> chooseStrategy(HasPlansAndId<PL, AG> person, String subpopulation, ReplanningContext replanningContext, StrategyChooser.Weights<PL, AG> weights) {
		double rnd = MatsimRandom.getRandom().nextDouble() * weights.getTotalWeights();

		double sum = 0.0;
		for (int i = 0, max = weights.size(); i < max; i++) {
			sum += weights.getWeight(i);
			if (rnd <= sum) {
				return weights.getStrategy(i);
			}
		}
		return null;
	}

}
