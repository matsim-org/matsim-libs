package org.matsim.modechoice.replanning;

import com.google.inject.Provider;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.modechoice.search.BestChoicesGenerator;

import javax.inject.Inject;

/**
 * Provider for {@link IMCSelectFromGenerator}.
 */
public class BestChoiceStrategyProvider implements Provider<PlanStrategy> {

	@Inject
	private javax.inject.Provider<TripRouter> tripRouterProvider;
	@Inject
	private GlobalConfigGroup globalConfigGroup;
	@Inject
	private ActivityFacilities facilities;
	@Inject
	private TimeInterpretation timeInterpretation;
	@Inject
	private BestChoicesGenerator generator;

	@Override
	public PlanStrategy get() {

		PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder(new RandomPlanSelector<>());

		// The generator will return the best choices, possibly multiple if there is uncertainty, so these will be randomly selected
		builder.addStrategyModule(new IMCSimpleStrategyModule(globalConfigGroup, generator, new RandomSelector(MatsimRandom.getLocalInstance())));
		builder.addStrategyModule(new ReRoute(facilities, tripRouterProvider, globalConfigGroup, timeInterpretation));

		return builder.build();
	}
}
