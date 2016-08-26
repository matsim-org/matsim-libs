package besttimeresponseintegration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.functions.ActivityUtilityParameters;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;
import org.matsim.core.utils.misc.Time;

import besttimeresponse.PlannedActivity;
import besttimeresponse.TimeAllocator;
import besttimeresponse.TripTravelTimes;
import opdytsintegration.utils.TimeDiscretization;

/**
 * 
 * @author Gunnar Flötteröd, based on MATSim example code
 *
 */
class BestTimeResponseStrategyModule implements PlanStrategyModule {

	// -------------------- MEMBERS --------------------

	private final Scenario scenario;

	private final Map<String, TravelTime> mode2tt;

	private final CharyparNagelScoringParametersForPerson scoringParams;

	private final TimeDiscretization timeDiscretization;

	private final TripTravelTimes myTravelTime;

	// -------------------- CONSTRUCTION --------------------

	BestTimeResponseStrategyModule(final Scenario scenario, final Map<String, TravelTime> mode2tt,
			final CharyparNagelScoringParametersForPerson scoringParams, final TimeDiscretization timeDiscretization,
			final TripTravelTimes myTravelTime) {
		this.scenario = scenario;
		this.mode2tt = mode2tt;
		this.scoringParams = scoringParams;
		this.timeDiscretization = timeDiscretization;
		this.myTravelTime = myTravelTime;
	}

	// --------------- IMPLEMENTATION OF PlanStrategyModule ---------------

	@Override
	public void handlePlan(final Plan plan) {

		System.out.println(">>>>> HANDLING PLAN FOR PERSON " + plan.getPerson());
		System.out.println("scenario = " + scenario);
		System.out.println("mode2tt = " + mode2tt + ", keys = " + mode2tt.keySet());
		System.out.println("scoringParams = " + scoringParams + ", for person = "
				+ scoringParams.getScoringParameters(plan.getPerson()));

		if (plan.getPlanElements().size() <= 1) {
			return; // nothing to do when just staying at home
		}

//		for (int r = 1; r < plan.getPlanElements().size(); r += 2) {
//			final Leg leg = (Leg) plan.getPlanElements().get(r);
//			if (!"car".equals(leg.getMode())) {
//				return;
//			}
//		}

		/*
		 * BUILD DATA STRUCTURES
		 */

		final List<PlannedActivity> plannedActivities = new ArrayList<>();
		final List<Double> dptTimes_s = new ArrayList<>();

		// Every other element is an activity; skip the last home activity.
		for (int q = 0; q < plan.getPlanElements().size() - 1; q += 2) {

			final Activity matsimAct = (Activity) plan.getPlanElements().get(q);
			final ActivityUtilityParameters matsimActParams = this.scoringParams
					.getScoringParameters(plan.getPerson()).utilParams.get(matsimAct.getType());
			final Leg matsimNextLeg = (Leg) plan.getPlanElements().get(q + 1);

			final Double latestStartTime_s = (matsimActParams.getLatestStartTime() == Time.UNDEFINED_TIME ? null
					: matsimActParams.getLatestStartTime());
			final Double earliestEndTime_s = (matsimActParams.getEarliestEndTime() == Time.UNDEFINED_TIME ? null
					: matsimActParams.getEarliestEndTime());
			final PlannedActivity plannedAct;
			if (q == 0) {
				plannedAct = PlannedActivity.newOvernightActivity(
						this.scenario.getNetwork().getLinks().get(matsimAct.getLinkId()).getToNode(),
						matsimNextLeg.getMode(), matsimActParams.getTypicalDuration(), latestStartTime_s,
						earliestEndTime_s);
			} else {
				final Double openingTime_s = (matsimActParams.getOpeningTime() == Time.UNDEFINED_TIME ? null
						: matsimActParams.getOpeningTime());
				final Double closingTime_s = (matsimActParams.getClosingTime() == Time.UNDEFINED_TIME ? null
						: matsimActParams.getClosingTime());
				plannedAct = PlannedActivity.newWithinDayActivity(
						this.scenario.getNetwork().getLinks().get(matsimAct.getLinkId()).getToNode(),
						matsimNextLeg.getMode(), matsimActParams.getTypicalDuration(), latestStartTime_s,
						earliestEndTime_s, openingTime_s, closingTime_s);
			}
			plannedActivities.add(plannedAct);
			dptTimes_s.add(matsimAct.getEndTime());
		}

		/*
		 * RUN OPTIMIZATION
		 */

		final TimeAllocator timeAlloc = new TimeAllocator(this.timeDiscretization, this.myTravelTime,
				this.scoringParams.getScoringParameters(plan.getPerson()).marginalUtilityOfPerforming_s,
				this.scoringParams.getScoringParameters(plan.getPerson()).modeParams
						.get("car").marginalUtilityOfTraveling_s,
				this.scoringParams.getScoringParameters(plan.getPerson()).marginalUtilityOfLateArrival_s,
				this.scoringParams.getScoringParameters(plan.getPerson()).marginalUtilityOfEarlyDeparture_s);

		final List<Double> result = timeAlloc.optimizeDepartureTimes(plannedActivities, dptTimes_s);
		System.out.println("INITIAL DPT TIMES: " + dptTimes_s);
		System.out.println("FINAL DPT TIMES: " + result);
		
		/*
		 * TAKE OVER DEPARTURE TIMES AND RECOMPUTE PATHS
		 */
		int i = 0;
		for (int q = 0; q < plan.getPlanElements().size() - 1; q += 2) {
			final double dptTime_s = result.get(i++);
			
			final Activity matsimAct = (Activity) plan.getPlanElements().get(q);
			matsimAct.setEndTime(dptTime_s);
			
			final Leg leg = (Leg) plan.getPlanElements().get(q + 1);
			if ("car".equals(leg.getMode())) {

				// NOW SOMEHOW GET THE NEW ROUTE IN HERE
				
			}
		}
	}

	@Override
	public void finishReplanning() {
	}

	@Override
	public void prepareReplanning(ReplanningContext replanningContext) {
	}
}
