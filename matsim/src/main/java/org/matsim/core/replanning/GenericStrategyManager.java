package org.matsim.core.replanning;

import org.matsim.api.core.v01.population.BasicPlan;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.core.api.internal.MatsimManager;
import org.matsim.core.replanning.selectors.PlanSelector;

import java.util.List;

public interface GenericStrategyManager<PL extends BasicPlan, AG extends HasPlansAndId<? extends BasicPlan, AG>> extends MatsimManager{
	void addStrategy(
			GenericPlanStrategy<PL, AG> strategy,
			String subpopulation,
			double weight );
	void run(
			Iterable<? extends HasPlansAndId<PL, AG>> persons,
			int iteration,
			ReplanningContext replanningContext );
	void setMaxPlansPerAgent( int maxPlansPerAgent );
	void addChangeRequest(
			int iteration,
			GenericPlanStrategy<PL, AG> strategy,
			String subpopulation,
			double newWeight );
	void setPlanSelectorForRemoval( PlanSelector<PL, AG> planSelector );
	List<GenericPlanStrategy<PL, AG>> getStrategies( String subpopulation );
	List<Double> getWeights( String subpopulation );
}
