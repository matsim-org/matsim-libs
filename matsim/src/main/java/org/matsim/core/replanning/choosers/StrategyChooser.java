package org.matsim.core.replanning.choosers;

import org.matsim.api.core.v01.population.BasicPlan;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.ReplanningContext;

/**
 * Interface for choosing a strategy for each person, each iteration.
 */
public interface StrategyChooser<T extends BasicPlan, I extends HasPlansAndId<? extends BasicPlan, I>> {

	default void beforeReplanning(ReplanningContext replanningContext) {
	}

	GenericPlanStrategy<T, I> chooseStrategy(HasPlansAndId<T, I> person, final String subpopulation, ReplanningContext replanningContext, Weights<T,I> weights);


	interface Weights<T extends BasicPlan, I> {


		int size();

		double getWeight(int idx);

		GenericPlanStrategy<T, I> getStrategy(int idx);

		double getTotalWeights();

	}
}
