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
	private final boolean permute;

	/**
	 * Constructor.
	 *
	 * @param iter    Force to use an innovative strategy every nth iteration
	 * @param permute Permute which agents are selected at which iteration.
	 *                This requires a sufficiently large number of agents (> 10.000) or the amount of agents forced to innovate each iteration may deviate from the mean.
	 */
	public ForceInnovationStrategyChooser(int iter, Permute permute) {
		this.iter = iter;
		this.permute = permute == Permute.yes;
	}

	/**
	 * Hash function for an integer
	 */
	static int hash(final int value) {
		int x = value;

		x = ((x >>> 16) ^ x) * 0x119de1f3;
		x = ((x >>> 16) ^ x) * 0x119de1f3;
		x = (x >>> 16) ^ x;

		return x;
	}

	@Override
	public GenericPlanStrategy<PL, AG> chooseStrategy(HasPlansAndId<PL, AG> person, String subpopulation, ReplanningContext replanningContext, Weights<PL, AG> weights) {

		double[] w = new double[weights.size()];
		double total = 0;

		// permutation term so the selection is different every nth iterations
		int perm = (replanningContext.getIteration() / iter) % 16;

		for (int i = 0; i < weights.size(); i++) {


			int term = (permute ? hash(person.getId().index()) >>> perm : person.getId().index()) % iter;
			int mod = replanningContext.getIteration() % iter;

			// Set selectors weight to zero, so only innovate strategies remain
			if ((term == mod || -term == mod) && ReplanningUtils.isOnlySelector(weights.getStrategy(i))) {
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

	public enum Permute {
		yes,
		no
	}

}
