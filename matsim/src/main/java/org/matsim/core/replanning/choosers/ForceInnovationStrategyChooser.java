package org.matsim.core.replanning.choosers;

import org.matsim.api.core.v01.population.BasicPlan;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.ReplanningUtils;

/**
 * This chooser forces to select an innovative strategy every X iteration for every X person in the population.
 */
public class ForceInnovationStrategyChooser<PL extends BasicPlan, AG extends HasPlansAndId<? extends BasicPlan, AG>> implements StrategyChooser<PL, AG> {

	private final int iter;

	/**
	 * Constructor.
	 *
	 * @param iter Force to use a innovative strategy every nth iteration
	 */
	public ForceInnovationStrategyChooser(int iter) {
		this.iter = iter;
	}

	@Override
	public GenericPlanStrategy<PL, AG> chooseStrategy(HasPlansAndId<PL, AG> person, String subpopulation, ReplanningContext replanningContext, Weights<PL, AG> weights) {

		double[] w = new double[weights.size()];
		double total = 0;

		for (int i = 0; i < weights.size(); i++) {

			// Use zero weight every nth iteration
			if (person.getId().index() % iter == replanningContext.getIteration() % iter && ReplanningUtils.isOnlySelector(weights.getStrategy(i))) {
				w[i] = 0;
			} else
				w[i] = weights.getWeight(i);

			total += w[i];
		}

		double rnd = MatsimRandom.getRandom().nextDouble() * total;

		// If all weights are zero the first one is returned
		double sum = 0.0;
		for (int i = 0, max = weights.size(); i < max; i++) {
			sum += w[i];
			if (rnd <= sum) {
				return weights.getStrategy(i);
			}
		}
		return null;

	}

}
