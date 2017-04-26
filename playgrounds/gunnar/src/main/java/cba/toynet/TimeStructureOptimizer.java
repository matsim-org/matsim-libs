package cba.toynet;

import java.util.Map;

import javax.inject.Provider;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.functions.SubpopulationScoringParameters;
import org.matsim.facilities.Facility;

import besttimeresponse.TimeAllocator;
import besttimeresponse.TripTravelTimes;
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

	public static enum LOGIC {
		sampers, matsim
	};

	// -------------------- CONSTANTS --------------------

	private final LOGIC logic;

	private final Scenario scenario;

	private final TimeDiscretization timeDiscretization;

	private final SubpopulationScoringParameters scoringParams;

	final Provider<TripRouter> tripRouterProvider;

	private final int maxTrials;

	private final int maxFailures;

	private final Map<String, TravelTime> mode2travelTime;

	private final Map<String, TravelTime> freeFlowMode2travelTime;

	final double betaTravelSampers_1_h;

	final SampersCarDelay sampersCarDelay;

	// -------------------- CONSTRUCTION --------------------

	TimeStructureOptimizer(final LOGIC logic, final Scenario scenario, final Provider<TripRouter> tripRouterProvider,
			final int maxTrials, final int maxFailures, final Map<String, TravelTime> mode2travelTime,
			final Map<String, TravelTime> mode2freeFlowTravelTime, final double betaTravelSampers_1_h,
			final SampersCarDelay sampersCarDelay) {
		this.logic = logic;
		this.scenario = scenario;
		this.timeDiscretization = TimeDiscretizationFactory.newInstance(scenario.getConfig());
		this.scoringParams = new SubpopulationScoringParameters(scenario);
		this.tripRouterProvider = tripRouterProvider;
		this.maxTrials = maxTrials;
		this.maxFailures = maxFailures;
		this.mode2travelTime = mode2travelTime;
		this.freeFlowMode2travelTime = mode2freeFlowTravelTime;
		this.betaTravelSampers_1_h = betaTravelSampers_1_h;
		this.sampersCarDelay = sampersCarDelay;
	}

	// -------------------- IMPLEMENTATION --------------------

	double computeScoreAndSetDepartureTimes(final Plan plan, final TourSequence tourSequence) {

		/*
		 * Define how trip travel times are computed.
		 */

		final TripTravelTimes<Facility, String> travelTimes;
		if (LOGIC.matsim.equals(this.logic)) {
			// use simulated travel times
			travelTimes = new BestTimeResponseTravelTimes(this.scenario.getNetwork(), this.timeDiscretization,
					this.tripRouterProvider.get(), plan.getPerson(), this.mode2travelTime);
			// TODO HERE ONE HAS AN INSTANCE OF AverageTravelTimeAcrossRuns IN mode2travelTime. OK!!!
		} else if (LOGIC.sampers.equals(this.logic)) {
			// use free-flow travel times
			travelTimes = new BestTimeResponseTravelTimes(this.scenario.getNetwork(), this.timeDiscretization,
					this.tripRouterProvider.get(), plan.getPerson(), this.freeFlowMode2travelTime);
		} else {
			throw new RuntimeException("Unknown logic: " + this.logic);
		}

		/*
		 * Find optimal time structure given travel times.
		 */

		final BestTimeResponseStrategyFunctionality initialPlanData = new BestTimeResponseStrategyFunctionality(plan,
				this.scenario.getNetwork(), this.scoringParams, this.timeDiscretization, travelTimes);
		final TimeAllocator<Facility, String> timeAlloc = initialPlanData.getTimeAllocator();
		// timeAlloc.setVerbose(LOGIC.matsim.equals(this.logic));
		timeAlloc.optimizeDepartureTimes(initialPlanData.plannedActivities, this.maxTrials, this.maxFailures);

		/*
		 * Encode optimal time structure in MATSim travel plan.
		 */

		for (int i = 0; i < timeAlloc.getResultPoint().length; i++) {
			final Activity act = (Activity) plan.getPlanElements().get(2 * i);
			act.setEndTime(timeAlloc.getResultPoint()[i]);
		}

		/*
		 * Return plan utility, either consistently with MATSim or naive.
		 */

		if (LOGIC.matsim.equals(this.logic)) {
			// if ((this.tripRouterProvider != null) && (this.mode2travelTime !=
			// null)) {

			// a proper departure time optimization has been run: consistent tts
			return timeAlloc.getResultValue();

		} else if (LOGIC.sampers.equals(this.logic)) {
			// } else if ((this.tripRouterProvider == null) &&
			// (this.mode2travelTime == null)) {

			/*
			 * First addend: (constant) utility based on free-flow conditions.
			 */

			double result = timeAlloc.getResultValue();

			/*
			 * Second addend: coefficient times delay.
			 */

			// System.out.println("\tDELAY UTILITY = " + this.betaTravelSampers_1_h * this.sampersCarDelay.getDelay_h(tourSequence));
			
			result += this.betaTravelSampers_1_h * this.sampersCarDelay.getDelay_h(tourSequence);

			return result;

		} else {
			throw new RuntimeException();
		}
	}
}
