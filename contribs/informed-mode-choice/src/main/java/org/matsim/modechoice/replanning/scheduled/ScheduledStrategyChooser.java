package org.matsim.modechoice.replanning.scheduled;

import com.google.inject.Inject;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.ReplanningUtils;
import org.matsim.core.replanning.choosers.StrategyChooser;
import org.matsim.core.replanning.choosers.WeightedStrategyChooser;
import org.matsim.core.replanning.modules.GenericPlanStrategyModule;
import org.matsim.modechoice.ScheduledModeChoiceConfigGroup;

/**
 * Chosen innovative strategies according to a fixed pre-determined schedule.
 */
public final class ScheduledStrategyChooser implements StrategyChooser<Plan, Person> {

	private static final int ITER_WARMUP = -1;
	private static final int ITER_BETWEEN = -2;
	private static final int ITER_OVER = -3;

	private final ScheduledModeChoiceConfigGroup scheduleConfig;

	private final WeightedStrategyChooser<Plan, Person> delegate;

	@Inject
	public ScheduledStrategyChooser(Config config) {
		this.scheduleConfig = ConfigUtils.addOrGetModule(config, ScheduledModeChoiceConfigGroup.class);
		this.delegate = new WeightedStrategyChooser<>();
	}

	/**
	 * Return the index of the schedule if it should be applied, -1 otherwise. -2 if the schedule is over.
	 */
	public static int isScheduledIteration(ReplanningContext replanningContext, ScheduledModeChoiceConfigGroup config) {

		int it = replanningContext.getIteration();
		if (it < config.getWarumUpIterations())
			return ITER_WARMUP;

		it -= config.getWarumUpIterations();

		int perIt = config.getBetweenIterations() + 1;

		if (it % perIt == 0) {
			int idx = it / perIt;

			if (idx >= config.getScheduleIterations())
				return ITER_OVER;

			return idx;
		}

		return ITER_BETWEEN;
	}

	private static boolean isScheduledStrategy(GenericPlanStrategy<Plan, Person> strategy) {
		if (strategy instanceof PlanStrategyImpl impl) {
			GenericPlanStrategyModule<Plan> first = impl.getFirstModule();
			return first instanceof AllBestPlansStrategy;
		}
		return false;
	}

	@Override
	public GenericPlanStrategy<Plan, Person> chooseStrategy(HasPlansAndId<Plan, Person> person, String subpopulation, ReplanningContext replanningContext, Weights<Plan, Person> weights) {

		// Not configured subpopulations are ignored
		if (!scheduleConfig.getSubpopulations().contains(subpopulation))
			return delegate.chooseStrategy(person, subpopulation, replanningContext, weights);

		double[] w = new double[weights.size()];
		double total = 0;

		int iterType = isScheduledIteration(replanningContext, scheduleConfig);

		for (int i = 0; i < weights.size(); i++) {

			GenericPlanStrategy<Plan, Person> strategy = weights.getStrategy(i);

			boolean isSchedule = isScheduledStrategy(strategy);
			boolean isSelector = ReplanningUtils.isOnlySelector(weights.getStrategy(i));

			// selectors are disabled in between iterations, agent should stay on the same plan type
			if (iterType == ITER_BETWEEN && isSelector)
				w[i] = 0;
			// all other than scheduler are disabled every other iteration
			else if (iterType >= 0 && !isSchedule)
				w[i] = 0;
			else
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
