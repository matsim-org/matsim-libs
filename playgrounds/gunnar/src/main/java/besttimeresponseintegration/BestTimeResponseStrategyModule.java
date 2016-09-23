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
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;

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

	private final ExperiencedScoreAnalyzer experiencedScoreAnalyzer;

	private final TravelTime carTravelTime;

	// -------------------- CONSTRUCTION --------------------

	BestTimeResponseStrategyModule(final Scenario scenario, final CharyparNagelScoringParametersForPerson scoringParams,
			final TimeDiscretization timeDiscretization, final ExperiencedScoreAnalyzer experiencedScoreAnalyzer,
			final TravelTime carTravelTime) {
		this.scenario = scenario;
		this.scoringParams = scoringParams;
		this.timeDiscretization = timeDiscretization;
		this.experiencedScoreAnalyzer = experiencedScoreAnalyzer;
		this.carTravelTime = carTravelTime;
	}

	// --------------- IMPLEMENTATION OF PlanStrategyModule ---------------

	@Override
	public void handlePlan(final Plan plan) {

		if (plan.getPlanElements().size() <= 1) {
			return; // nothing to do when just staying at home
		}

		final boolean interpolate = true;

		final BestTimeResponseStrategyFunctionality initialPlanData = new BestTimeResponseStrategyFunctionality(plan,
				this.scenario.getNetwork(), this.scoringParams, this.timeDiscretization, this.carTravelTime,
				interpolate);
		final TimeAllocator<Link, String> timeAlloc = initialPlanData.getTimeAllocator();

		final double[] initialDptTimesArray_s = new double[initialPlanData.initialDptTimes_s.size()];
		for (int q = 0; q < initialPlanData.initialDptTimes_s.size(); q++) {
			initialDptTimesArray_s[q] = initialPlanData.initialDptTimes_s.get(q);
		}
		final double[] result = timeAlloc.optimizeDepartureTimes(initialPlanData.plannedActivities,
				initialDptTimesArray_s);
		System.out.println("FINAL DPT TIMES: " + new ArrayRealVector(result));

		// >>>>>>>>>> TODO NEW >>>>>>>>>>
		this.experiencedScoreAnalyzer.setExpectedScore(plan.getPerson().getId(), timeAlloc.getResultValue());
		// <<<<<<<<<< TODO NEW <<<<<<<<<<

		/*
		 * TAKE OVER DEPARTURE TIMES AND RECOMPUTE PATHS
		 * 
		 * TODO Recomputation is inefficient.
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

				final Path path = initialPlanData.getTravelTimes().getCarPath(fromLink, toLink, dptTime_s);

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
