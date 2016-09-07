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

import floetteroed.utilities.Units;
import opdytsintegration.utils.TimeDiscretization;

/**
 * 
 * @author Gunnar Flötteröd, based on MATSim example code
 *
 */
public class BestTimeResponseStrategyProvider implements Provider<PlanStrategy> {

	// -------------------- MEMBERS --------------------

	private final boolean interpolate = true;

	private final PlanSelector<Plan, Person> randomPlanSelector;

	private final TimeDiscretization timeDiscr;

	private final Scenario scenario;

	private final CharyparNagelScoringParametersForPerson scoringParams;

	private final BestTimeResponseTravelTimes myTravelTime;

	// -------------------- CONSTRUCTION --------------------

	@Inject
	BestTimeResponseStrategyProvider(final Scenario scenario, final Map<String, TravelTime> mode2tt,
			final CharyparNagelScoringParametersForPerson scoringParams) {
		this.randomPlanSelector = new RandomPlanSelector<>();

		final int startTime_s = 0;
		final int binSize_s = scenario.getConfig().travelTimeCalculator().getTraveltimeBinSize();
		final int binCnt = (int) Math.ceil(Units.S_PER_D / binSize_s);
		this.timeDiscr = new TimeDiscretization(startTime_s, binSize_s, binCnt);
		this.myTravelTime = new BestTimeResponseTravelTimes(this.timeDiscr, mode2tt, scenario.getNetwork(),
				this.interpolate);

		this.scenario = scenario;
		this.scoringParams = scoringParams;
	}

	// --------------- IMPLEMENTATION OF Provider<PlanStrategy> ---------------

	@Override
	public PlanStrategy get() {
		final PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder(this.randomPlanSelector);
		final BestTimeResponseStrategyModule module = new BestTimeResponseStrategyModule(this.scenario,
				this.scoringParams, this.timeDiscr, this.myTravelTime);
		builder.addStrategyModule(module);
		return builder.build();
	}
}
