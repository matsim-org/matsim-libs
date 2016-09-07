package besttimeresponseintegration;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.scoring.functions.ActivityUtilityParameters;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;
import org.matsim.core.utils.misc.Time;

import besttimeresponse.PlannedActivity;
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

	private final BestTimeResponseTravelTimes myTravelTime;

	// -------------------- CONSTRUCTION --------------------

	BestTimeResponseStrategyModule(final Scenario scenario, final CharyparNagelScoringParametersForPerson scoringParams,
			final TimeDiscretization timeDiscretization, final BestTimeResponseTravelTimes myTravelTime) {
		this.scenario = scenario;
		this.scoringParams = scoringParams;
		this.timeDiscretization = timeDiscretization;
		this.myTravelTime = myTravelTime;
	}

	// --------------- IMPLEMENTATION OF PlanStrategyModule ---------------

	@Override
	public void handlePlan(final Plan plan) {

		if (plan.getPlanElements().size() <= 1) {
			return; // nothing to do when just staying at home
		}

		/*
		 * BUILD DATA STRUCTURES
		 */

		final List<PlannedActivity<Link, String>> plannedActivities = new ArrayList<>(
				plan.getPlanElements().size() / 2);
		final List<Double> initialDptTimes_s = new ArrayList<>(plan.getPlanElements().size() / 2);

		// Every other element is an activity; skip the last home activity.
		for (int q = 0; q < plan.getPlanElements().size() - 1; q += 2) {

			final Activity matsimAct = (Activity) plan.getPlanElements().get(q);
			final ActivityUtilityParameters matsimActParams = this.scoringParams
					.getScoringParameters(plan.getPerson()).utilParams.get(matsimAct.getType());
			final Leg matsimNextLeg = (Leg) plan.getPlanElements().get(q + 1);

			initialDptTimes_s.add(matsimAct.getEndTime());

			final Double openingTime_s = ((matsimActParams.getOpeningTime() == Time.UNDEFINED_TIME) ? null
					: matsimActParams.getOpeningTime());
			final Double closingTime_s = ((matsimActParams.getClosingTime() == Time.UNDEFINED_TIME) ? null
					: matsimActParams.getClosingTime());
			final Double latestStartTime_s = ((matsimActParams.getLatestStartTime() == Time.UNDEFINED_TIME) ? null
					: matsimActParams.getLatestStartTime());
			final Double earliestEndTime_s = ((matsimActParams.getEarliestEndTime() == Time.UNDEFINED_TIME) ? null
					: matsimActParams.getEarliestEndTime());
			final PlannedActivity<Link, String> plannedAct = new PlannedActivity<Link, String>(
					this.scenario.getNetwork().getLinks().get(matsimAct.getLinkId()), matsimNextLeg.getMode(),
					matsimActParams.getTypicalDuration(), openingTime_s, closingTime_s, latestStartTime_s,
					earliestEndTime_s);
			plannedActivities.add(plannedAct);
		}

		/*
		 * RUN OPTIMIZATION
		 */

		final boolean repairTimeStructure = true;
		final boolean interpolateTravelTimes = true;
		final boolean randomSmoothing = true;

		final TimeAllocator<Link, String> timeAlloc = new TimeAllocator<>(this.timeDiscretization, this.myTravelTime,
				this.scoringParams.getScoringParameters(plan.getPerson()).marginalUtilityOfPerforming_s,
				this.scoringParams.getScoringParameters(plan.getPerson()).modeParams
						.get("car").marginalUtilityOfTraveling_s,
				this.scoringParams.getScoringParameters(plan.getPerson()).marginalUtilityOfLateArrival_s,
				this.scoringParams.getScoringParameters(plan.getPerson()).marginalUtilityOfEarlyDeparture_s,
				repairTimeStructure, interpolateTravelTimes, randomSmoothing);

		final double[] result = timeAlloc.optimizeDepartureTimes(plannedActivities, null);
		System.out.println("FINAL DPT TIMES: " + new ArrayRealVector(result));

		/*
		 * TAKE OVER DEPARTURE TIMES AND RECOMPUTE PATHS
		 */
		int i = 0;
		for (int q = 0; q < plan.getPlanElements().size() - 1; q += 2) {
			final double dptTime_s = result[i++];

			final Activity matsimAct = (Activity) plan.getPlanElements().get(q);
			matsimAct.setEndTime(dptTime_s);
			final Link fromLink = this.scenario.getNetwork().getLinks().get(matsimAct.getLinkId());

			final Leg leg = (Leg) plan.getPlanElements().get(q + 1);
			if ("car".equals(leg.getMode())) {

				final Activity nextMATSimAct = (Activity) plan.getPlanElements().get(q + 2);
				final Link toLink = this.scenario.getNetwork().getLinks().get(nextMATSimAct.getLinkId());

				final Path path = this.myTravelTime.getCarPath(fromLink, toLink, dptTime_s);
				List<Id<Link>> linkIds = new ArrayList<>(path.links.size());
				for (Link link : path.links) {
					linkIds.add(link.getId());
				}
				((NetworkRoute) leg.getRoute()).setLinkIds(matsimAct.getLinkId(), linkIds, nextMATSimAct.getLinkId());
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
