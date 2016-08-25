package besttimeresponseintegration;

import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.functions.ActivityUtilityParameters;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;
import org.matsim.core.utils.misc.Time;

import besttimeresponse.PlannedActivity;
import besttimeresponse.RealizedActivitiesBuilder;
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

	private final BestTimeResponseTravelTimes myTravelTime;

	// -------------------- CONSTRUCTION --------------------

	BestTimeResponseStrategyModule(final Scenario scenario, final Map<String, TravelTime> mode2tt,
			final CharyparNagelScoringParametersForPerson scoringParams, final TimeDiscretization timeDiscretization,
			final BestTimeResponseTravelTimes myTravelTime) {
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

		final RealizedActivitiesBuilder actBuilder = new RealizedActivitiesBuilder(this.timeDiscretization,
				this.myTravelTime, true);

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
				plannedAct = PlannedActivity.newOvernightActivity(matsimAct.getLinkId(), matsimNextLeg.getMode(),
						matsimActParams.getTypicalDuration(), latestStartTime_s, earliestEndTime_s);
			} else {
				final Double openingTime_s = (matsimActParams.getOpeningTime() == Time.UNDEFINED_TIME ? null
						: matsimActParams.getOpeningTime());
				final Double closingTime_s = (matsimActParams.getClosingTime() == Time.UNDEFINED_TIME ? null
						: matsimActParams.getClosingTime());
				plannedAct = PlannedActivity.newWithinDayActivity(matsimAct.getLinkId(), matsimNextLeg.getMode(),
						matsimActParams.getTypicalDuration(), latestStartTime_s, earliestEndTime_s, openingTime_s,
						closingTime_s);
			}

			actBuilder.addActivity(plannedAct, matsimAct.getEndTime());
		}

	}

	@Override
	public void finishReplanning() {
	}

	@Override
	public void prepareReplanning(ReplanningContext replanningContext) {
	}
}
