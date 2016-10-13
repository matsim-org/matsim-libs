package besttimeresponseintegration;

import javax.inject.Inject;
import javax.inject.Provider;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.selectors.PlanSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;
import org.matsim.facilities.ActivityFacilities;

import matsimintegration.TimeDiscretizationInjection;
import opdytsintegration.utils.TimeDiscretization;

/**
 * 
 * @author Gunnar Flötteröd, based on MATSim example code
 *
 */
public class BestTimeResponseStrategyProvider implements Provider<PlanStrategy> {

	// -------------------- MEMBERS --------------------

	private final PlanSelector<Plan, Person> randomPlanSelector;

	private final TimeDiscretization timeDiscr;

	private final Scenario scenario;

	private final CharyparNagelScoringParametersForPerson scoringParams;

	// private final ExperiencedScoreAnalyzer experiencedScoreAnalyzer;

	private final Provider<TripRouter> tripRouterProvider;

	@Inject
	private GlobalConfigGroup globalConfigGroup;
	@Inject
	private ActivityFacilities facilities;

	private final boolean reRouteBefore = false; // useless anyway
	private final boolean reRouteAfter = true;
	private final int maxTrials = 10;
	private final int maxFailures = 3;

	// -------------------- CONSTRUCTION --------------------

	@Inject
	BestTimeResponseStrategyProvider(final Scenario scenario,
			final CharyparNagelScoringParametersForPerson scoringParams,
			// final ExperiencedScoreAnalyzer experiencedScoreAnalyzer,
			final TimeDiscretizationInjection timeDiscrInj, final Provider<TripRouter> tripRouterProvider) {

		this.randomPlanSelector = new RandomPlanSelector<>();
		this.timeDiscr = timeDiscrInj.getInstance();
		this.scenario = scenario;
		this.scoringParams = scoringParams;
		// this.experiencedScoreAnalyzer = experiencedScoreAnalyzer;
		this.tripRouterProvider = tripRouterProvider;
	}

	// --------------- IMPLEMENTATION OF Provider<PlanStrategy> ---------------

	@Override
	public PlanStrategy get() {
		final PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder(this.randomPlanSelector);
		final BestTimeResponseStrategyModule module = new BestTimeResponseStrategyModule(this.scenario,
				this.scoringParams, this.timeDiscr, // this.experiencedScoreAnalyzer,
				this.tripRouterProvider.get(), this.maxTrials, this.maxFailures);
		if (this.reRouteBefore) {
			builder.addStrategyModule(
					new org.matsim.core.replanning.modules.ReRoute(facilities, tripRouterProvider, globalConfigGroup));
		}
		builder.addStrategyModule(module);
		if (this.reRouteAfter) {
			builder.addStrategyModule(
					new org.matsim.core.replanning.modules.ReRoute(facilities, tripRouterProvider, globalConfigGroup));
		}
		return builder.build();
	}
}
