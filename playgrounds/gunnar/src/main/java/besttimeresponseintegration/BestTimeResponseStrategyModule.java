package besttimeresponseintegration;

import java.util.Map;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;
import org.matsim.facilities.Facility;

import besttimeresponse.TimeAllocator;
import opdytsintegration.utils.TimeDiscretization;

/**
 * 
 * @author Gunnar Flötteröd, based on MATSim example code
 *
 */
class BestTimeResponseStrategyModule implements PlanStrategyModule {

	// -------------------- MEMBERS --------------------

	private final Scenario scenario;

	private final CharyparNagelScoringParametersForPerson scoringParams;

	private final TimeDiscretization timeDiscretization;

	private final TripRouter tripRouter;

	private final Map<String, TravelTime> mode2travelTime;

	private final int maxTrials;

	private final int maxFailures;

	private final boolean verbose = false;

	// -------------------- CONSTRUCTION --------------------

	BestTimeResponseStrategyModule(final Scenario scenario, final CharyparNagelScoringParametersForPerson scoringParams,
			final TimeDiscretization timeDiscretization, final TripRouter tripRouter,
			final Map<String, TravelTime> mode2travelTime, final int maxTrials, final int maxFailures) {
		this.scenario = scenario;
		this.scoringParams = scoringParams;
		this.timeDiscretization = timeDiscretization;
		this.tripRouter = tripRouter;
		this.mode2travelTime = mode2travelTime;
		this.maxTrials = maxTrials;
		this.maxFailures = maxFailures;
	}

	// --------------- IMPLEMENTATION OF PlanStrategyModule ---------------

	@Override
	public void handlePlan(final Plan plan) {

		if (plan.getPlanElements().size() <= 1) {
			return; // nothing to do when just staying at home
		}

		final BestTimeResponseTravelTimes travelTimes = new BestTimeResponseTravelTimes(this.scenario.getNetwork(),
				this.timeDiscretization, this.tripRouter, plan.getPerson(), this.mode2travelTime);

		final BestTimeResponseStrategyFunctionality initialPlanData = new BestTimeResponseStrategyFunctionality(plan,
				this.scenario.getNetwork(), this.scoringParams, this.timeDiscretization, travelTimes);
		final TimeAllocator<Facility, String> timeAlloc = initialPlanData.getTimeAllocator();

		timeAlloc.optimizeDepartureTimes(initialPlanData.plannedActivities, this.maxTrials, this.maxFailures);
		final double[] result = timeAlloc.getResultPoint();

		// final double[] initialDptTimesArray_s = new
		// double[initialPlanData.initialDptTimes_s.size()];
		// for (int q = 0; q < initialPlanData.initialDptTimes_s.size(); q++) {
		// initialDptTimesArray_s[q] = initialPlanData.initialDptTimes_s.get(q);
		// }
		// final double result[] =
		// timeAlloc.optimizeDepartureTimes(initialPlanData.plannedActivities,
		// initialDptTimesArray_s);

		if (this.verbose) {
			System.out.println("FINAL DPT TIMES: " + new ArrayRealVector(result));
		}

		/*
		 * TAKE OVER DEPARTURE TIMES
		 */
		int i = 0;
		for (int q = 0; q < plan.getPlanElements().size() - 1; q += 2) {
			final double dptTime_s = result[i++];
			final Activity matsimAct = (Activity) plan.getPlanElements().get(q);
			final Leg nextLeg = (Leg) plan.getPlanElements().get(q + 1);
			matsimAct.setEndTime(dptTime_s);
			nextLeg.setDepartureTime(dptTime_s);
		}
	}

	@Override
	public void finishReplanning() {
	}

	@Override
	public void prepareReplanning(ReplanningContext replanningContext) {
	}
}
