package besttimeresponseintegration;

import javax.inject.Inject;
import javax.inject.Provider;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.selectors.PlanSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;

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

	// -------------------- CONSTRUCTION --------------------

	@Inject
	BestTimeResponseStrategyProvider(final Scenario scenario,
			final CharyparNagelScoringParametersForPerson scoringParams,
			// final ExperiencedScoreAnalyzer experiencedScoreAnalyzer, 
			final TimeDiscretizationInjection timeDiscrInj,
			final Provider<TripRouter> tripRouterProvider) {
		
		this.randomPlanSelector = new RandomPlanSelector<>();
		this.timeDiscr = timeDiscrInj.getInstance();
		this.scenario = scenario;
		this.scoringParams = scoringParams;
//		this.experiencedScoreAnalyzer = experiencedScoreAnalyzer;
		this.tripRouterProvider = tripRouterProvider;
	}

	// --------------- IMPLEMENTATION OF Provider<PlanStrategy> ---------------

	@Override
	public PlanStrategy get() {
		final PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder(this.randomPlanSelector);
		final BestTimeResponseStrategyModule module = new BestTimeResponseStrategyModule(this.scenario,
				this.scoringParams, this.timeDiscr, // this.experiencedScoreAnalyzer, 
				this.tripRouterProvider.get());
		builder.addStrategyModule(module);
		return builder.build();
	}
}
