package besttimeresponseintegration;

import java.util.Map;

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
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
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

	private final ScoringParametersForPerson scoringParams;

	private final Provider<TripRouter> tripRouterProvider;

	private final Map<String, TravelTime> mode2travelTime;

	private final GlobalConfigGroup globalConfigGroup;

	private final ActivityFacilities facilities;

	private final int maxTrials;

	private final int maxFailures;

	// -------------------- CONSTRUCTION --------------------

	@Inject
	BestTimeResponseStrategyProvider(final Scenario scenario,
			final ScoringParametersForPerson scoringParams, final TimeDiscretizationInjection timeDiscrInj,
			final Provider<TripRouter> tripRouterProvider, final Map<String, TravelTime> mode2travelTime,
			final GlobalConfigGroup globalConfigGroup, final ActivityFacilities facilities) {

		this.randomPlanSelector = new RandomPlanSelector<>();
		this.timeDiscr = timeDiscrInj.getInstance();
		this.scenario = scenario;
		this.scoringParams = scoringParams;
		this.tripRouterProvider = tripRouterProvider;
		this.mode2travelTime = mode2travelTime;
		this.globalConfigGroup = globalConfigGroup;
		this.facilities = facilities;
		this.maxTrials = 10;
		this.maxFailures = 3;
	}

	// --------------- IMPLEMENTATION OF Provider<PlanStrategy> ---------------

	@Override
	public PlanStrategy get() {
		final PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder(this.randomPlanSelector);
		final BestTimeResponseStrategyModule module = new BestTimeResponseStrategyModule(this.scenario,
				this.scoringParams, this.timeDiscr, this.tripRouterProvider.get(), this.mode2travelTime, this.maxTrials,
				this.maxFailures);
		builder.addStrategyModule(module);
		builder.addStrategyModule(new org.matsim.core.replanning.modules.ReRoute(this.facilities,
				this.tripRouterProvider, this.globalConfigGroup));
		return builder.build();
	}
}
