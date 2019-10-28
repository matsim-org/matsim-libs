package playground.vsp.cadyts.marginals;

import cadyts.demand.PlanBuilder;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.cadyts.general.PlansTranslator;

class ModalDistancePlansTranslator implements PlansTranslator<Id<DistanceDistribution.DistanceBin>> {

	private static final Logger logger = Logger.getLogger(PlansTranslator.class);
	private static final String BUILDER_KEY = "cadytsPlanBuilder-marginals";
	private static final String ITERATION_KEY = "cadytsPlanBuilder-marinals-lastIteration";

	@SuppressWarnings("unchecked")
	@Override
	public cadyts.demand.Plan<Id<DistanceDistribution.DistanceBin>> getCadytsPlan(Plan plan) {

		if (plan.getCustomAttributes().containsKey(BUILDER_KEY)) {
			return ((PlanBuilder<Id<DistanceDistribution.DistanceBin>>) plan.getCustomAttributes().get(BUILDER_KEY)).getResult();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	void addTurn(Plan forPlan, Id<DistanceDistribution.DistanceBin> turn, int iteration) {

		// attach plan builder as an attribute to the plan. Would have preferred a local Hash map
		// but the count-cadyts implementation does it too. Therefore we do it here as well for consistency
		Integer lastIteration = (Integer) forPlan.getCustomAttributes().get(ITERATION_KEY);
		PlanBuilder<Id<DistanceDistribution.DistanceBin>> builder = (PlanBuilder<Id<DistanceDistribution.DistanceBin>>) forPlan.getCustomAttributes().get(BUILDER_KEY);
		if (lastIteration == null || builder == null || lastIteration != iteration) {

			builder = new PlanBuilder<>();
			forPlan.getCustomAttributes().put(BUILDER_KEY, builder);
			forPlan.getCustomAttributes().put(ITERATION_KEY, iteration);
		}

		builder.addTurn(turn, 1);
	}
}
