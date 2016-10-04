package cba;

import javax.inject.Provider;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.router.TripRouter;
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

	// private final TravelTime carTravelTime;

	private final SubpopulationCharyparNagelScoringParameters scoringParams;

	// private final BestTimeResponseTravelTimes travelTimes;

	final Provider<TripRouter> tripRouterProvider;

	// -------------------- CONSTRUCTION --------------------

	TimeStructureOptimizer(final Scenario scenario, final Provider<TripRouter> tripRouterProvider) {
		this.scenario = scenario;
		this.timeDiscretization = TimeDiscretizationFactory.newInstance(scenario.getConfig());
		// this.carTravelTime = carTravelTime;
		this.scoringParams = new SubpopulationCharyparNagelScoringParameters(scenario);

		// if (carTravelTime == null) {
		// this.travelTimes = null;
		// } else {
		// this.travelTimes = new
		// BestTimeResponseTravelTimes(this.timeDiscretization, carTravelTime,
		// scenario.getNetwork(), true);
		// this.travelTimes.setCaching(true);
		// }

		this.tripRouterProvider = tripRouterProvider;
	}

	// -------------------- IMPLEMENTATION --------------------

	double computeScore(final Plan plan) {

		final BestTimeResponseTravelTimes travelTimes = new BestTimeResponseTravelTimes(this.timeDiscretization,
				this.tripRouterProvider.get(), plan.getPerson());

		final BestTimeResponseStrategyFunctionality initialPlanData = new BestTimeResponseStrategyFunctionality(plan,
				this.scenario.getNetwork(), this.scoringParams, this.timeDiscretization,
				// carTravelTime, true, true
				travelTimes);

		final TimeAllocator<Facility, String> timeAlloc = initialPlanData.getTimeAllocator();

		final int maxTrials = 1;
		final int maxFailures = 1;
		timeAlloc.optimizeDepartureTimes(initialPlanData.plannedActivities, maxTrials, maxFailures);

		return timeAlloc.getResultValue();

	}
}
