package cba;

import java.util.Map;

import javax.inject.Provider;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.functions.SubpopulationCharyparNagelScoringParameters;
import org.matsim.facilities.Facility;

import besttimeresponse.TimeAllocator;
import besttimeresponseintegration.BestTimeResponseStrategyFunctionality;
import besttimeresponseintegration.BestTimeResponseTravelTimes;
import matsimintegration.TimeDiscretizationFactory;
import opdytsintegration.utils.TimeDiscretization;

/**
 * Computes optimal time structures for given travel plans.
 * 
 * @author Gunnar Flötteröd
 *
 */
class TimeStructureOptimizer {

	// -------------------- CONSTANTS --------------------

	private final Scenario scenario;

	private final TimeDiscretization timeDiscretization;

	private final SubpopulationCharyparNagelScoringParameters scoringParams;

	final Provider<TripRouter> tripRouterProvider;

	private final int maxTrials;

	private final int maxFailures;

	private final Map<String, TravelTime> mode2travelTime;

	// -------------------- CONSTRUCTION --------------------

	TimeStructureOptimizer(final Scenario scenario, final Provider<TripRouter> tripRouterProvider, final int maxTrials,
			final int maxFailures, final Map<String, TravelTime> mode2travelTime) {
		this.scenario = scenario;
		this.timeDiscretization = TimeDiscretizationFactory.newInstance(scenario.getConfig());
		this.scoringParams = new SubpopulationCharyparNagelScoringParameters(scenario);
		this.tripRouterProvider = tripRouterProvider;
		this.maxTrials = maxTrials;
		this.maxFailures = maxFailures;
		this.mode2travelTime = mode2travelTime;
	}

	// -------------------- IMPLEMENTATION --------------------

	double computeScore(final Plan plan) {

		final BestTimeResponseTravelTimes travelTimes = new BestTimeResponseTravelTimes(this.scenario.getNetwork(),
				this.timeDiscretization, this.tripRouterProvider.get(), plan.getPerson(), this.mode2travelTime);

		final BestTimeResponseStrategyFunctionality initialPlanData = new BestTimeResponseStrategyFunctionality(plan,
				this.scenario.getNetwork(), this.scoringParams, this.timeDiscretization, travelTimes);

		final TimeAllocator<Facility, String> timeAlloc = initialPlanData.getTimeAllocator();

		timeAlloc.optimizeDepartureTimes(initialPlanData.plannedActivities, this.maxTrials, this.maxFailures);

		for (int i = 0; i < timeAlloc.getResultPoint().length; i++) {
			final Activity act = (Activity) plan.getPlanElements().get(2 * i);
			act.setEndTime(timeAlloc.getResultPoint()[i]);
		}

		return timeAlloc.getResultValue();
	}
}
