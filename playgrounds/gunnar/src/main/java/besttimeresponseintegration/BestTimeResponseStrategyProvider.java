package besttimeresponseintegration;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.selectors.PlanSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.util.TravelTime;
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

	private final Map<String, TravelTime> mode2tt;

	private final CharyparNagelScoringParametersForPerson scoringParams;

	private final ExperiencedScoreAnalyzer experiencedScoreAnalyzer;

	// -------------------- CONSTRUCTION --------------------

	@Inject
	BestTimeResponseStrategyProvider(final Scenario scenario, final Map<String, TravelTime> mode2tt,
			final CharyparNagelScoringParametersForPerson scoringParams,
			final ExperiencedScoreAnalyzer experiencedScoreAnalyzer, TimeDiscretizationInjection timeDiscrInj) {
		this.randomPlanSelector = new RandomPlanSelector<>();
		this.timeDiscr = timeDiscrInj.getInstance();
		this.scenario = scenario;
		this.mode2tt = mode2tt;
		this.scoringParams = scoringParams;
		this.experiencedScoreAnalyzer = experiencedScoreAnalyzer;
	}

	// --------------- IMPLEMENTATION OF Provider<PlanStrategy> ---------------

	@Override
	public PlanStrategy get() {
		final PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder(this.randomPlanSelector);
		final BestTimeResponseStrategyModule module = new BestTimeResponseStrategyModule(this.scenario,
				this.scoringParams, this.timeDiscr, this.experiencedScoreAnalyzer, this.mode2tt);
		builder.addStrategyModule(module);
		return builder.build();
	}
}
