package besttimeresponseintegration;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.functions.ActivityUtilityParameters;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

import besttimeresponse.PlannedActivity;
import besttimeresponse.RealizedActivitiesBuilder;
import besttimeresponse.TravelTimes;
import opdytsintegration.utils.TimeDiscretization;

/**
 * 
 * @author Gunnar Flötteröd, based on MATSim example code
 *
 */
class BestTimeResponseStrategyModule implements PlanStrategyModule {

	private final Scenario scenario;

	private final TravelTime travelTime;

	final CharyparNagelScoringParameters scoringParams;
	
	public BestTimeResponseStrategyModule(final Scenario scenario, 
			final TravelTime travelTime,
			final CharyparNagelScoringParameters scoringParams) {

		this.scenario = scenario;
		this.travelTime = travelTime;	
		this.scoringParams = scoringParams;

	}

	@Override
	public void handlePlan(final Plan plan) {

		
		if (plan.getPlanElements().size() <= 1) {
			return; // nothing to do when just staying at home
		}

		final TimeDiscretization timeDiscr = null; // TODO
		final TravelTimes travelTimes = null; // TODO
		final RealizedActivitiesBuilder actBuilder = new RealizedActivitiesBuilder(timeDiscr, travelTimes, true);

		// Every other element is an activity; skip the last home activity.
		for (int q = 0; q < plan.getPlanElements().size() - 1; q += 2) {

			final Activity matsimAct = (Activity) plan.getPlanElements().get(q);
			final ActivityUtilityParameters matsimActParams = this.scoringParams.utilParams.get(matsimAct.getType());
			final Leg matsimNextLeg = (Leg) plan.getPlanElements().get(q + 1);

			final PlannedActivity plannedAct;
			if (q == 0) {
				plannedAct = PlannedActivity.newOvernightActivity(matsimAct.getLinkId(), matsimNextLeg.getMode(),
						matsimActParams.getTypicalDuration(), matsimActParams.getLatestStartTime(),
						matsimActParams.getEarliestEndTime());
			} else {
				plannedAct = PlannedActivity.newWithinDayActivity(matsimAct.getLinkId(), matsimNextLeg.getMode(),
						matsimActParams.getTypicalDuration(), matsimActParams.getLatestStartTime(),
						matsimActParams.getEarliestEndTime(), matsimActParams.getOpeningTime(),
						matsimActParams.getClosingTime());
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
