package besttimeresponseintegration;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.functions.ActivityUtilityParameters;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;
import org.matsim.core.utils.misc.Time;

import besttimeresponse.PlannedActivity;
import besttimeresponse.TimeAllocator;
import floetteroed.utilities.Units;
import opdytsintegration.utils.TimeDiscretization;

/**
 * This class separates the functionality of the BestTimeResponseStrategyModule
 * from that very module. This allows to access this functionality before it has
 * sunk into the MATSim/Guice machinery.
 * 
 * @author Gunnar Flötteröd
 *
 */
public class BestTimeResponseStrategyFunctionality {

	// -------------------- MEMBERS --------------------

	public final List<PlannedActivity<Link, String>> plannedActivities;

	public final List<Double> initialDptTimes_s;

	private final TimeAllocator<Link, String> timeAlloc;

	private final BestTimeResponseTravelTimes myTravelTimes;

	// -------------------- CONSTRUCTION --------------------

	public BestTimeResponseStrategyFunctionality(final Plan plan, final Network network,
			final CharyparNagelScoringParametersForPerson scoringParams, final TimeDiscretization timeDiscretization,
			final TravelTime carTravelTime, final boolean interpolate) {

		/*
		 * Building the initial plan data.
		 */

		if (plan.getPlanElements().size() <= 1) {
			throw new RuntimeException("Cannot compute initial plan data for a plan with less than two elements.");
		}

		this.plannedActivities = new ArrayList<>(plan.getPlanElements().size() / 2);
		this.initialDptTimes_s = new ArrayList<>(plan.getPlanElements().size() / 2);

		// Every other element is an activity; skip the last home activity.
		for (int q = 0; q < plan.getPlanElements().size() - 1; q += 2) {

			final Activity matsimAct = (Activity) plan.getPlanElements().get(q);

			final ActivityUtilityParameters matsimActParams;
			try {
				matsimActParams = scoringParams.getScoringParameters(plan.getPerson()).utilParams
						.get(matsimAct.getType());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

			final Leg matsimNextLeg = (Leg) plan.getPlanElements().get(q + 1);

			this.initialDptTimes_s.add(matsimAct.getEndTime());

			final Double openingTime_s = ((matsimActParams.getOpeningTime() == Time.UNDEFINED_TIME) ? null
					: matsimActParams.getOpeningTime());
			final Double closingTime_s = ((matsimActParams.getClosingTime() == Time.UNDEFINED_TIME) ? null
					: matsimActParams.getClosingTime());
			final Double latestStartTime_s = ((matsimActParams.getLatestStartTime() == Time.UNDEFINED_TIME) ? null
					: matsimActParams.getLatestStartTime());
			final Double earliestEndTime_s = ((matsimActParams.getEarliestEndTime() == Time.UNDEFINED_TIME) ? null
					: matsimActParams.getEarliestEndTime());
			final PlannedActivity<Link, String> plannedAct = new PlannedActivity<Link, String>(
					network.getLinks().get(matsimAct.getLinkId()), matsimNextLeg.getMode(),
					matsimActParams.getTypicalDuration(), Units.S_PER_H * matsimActParams.getZeroUtilityDuration_h(),
					openingTime_s, closingTime_s, latestStartTime_s, earliestEndTime_s);
			this.plannedActivities.add(plannedAct);
		}

		/*
		 * Evaluating the plan.
		 */

		final boolean repairTimeStructure = true;
		final boolean interpolateTravelTimes = true;
		final boolean randomSmoothing = true;

		this.myTravelTimes = new BestTimeResponseTravelTimes(timeDiscretization, carTravelTime, network, interpolate);
		this.timeAlloc = new TimeAllocator<>(timeDiscretization, this.myTravelTimes,
				scoringParams.getScoringParameters(plan.getPerson()).marginalUtilityOfPerforming_s,
				scoringParams.getScoringParameters(plan.getPerson()).modeParams.get("car").marginalUtilityOfTraveling_s,
				scoringParams.getScoringParameters(plan.getPerson()).marginalUtilityOfLateArrival_s,
				scoringParams.getScoringParameters(plan.getPerson()).marginalUtilityOfEarlyDeparture_s,
				repairTimeStructure, interpolateTravelTimes, randomSmoothing);
	}

	// -------------------- IMPLEMENTATION --------------------

	public TimeAllocator<Link, String> getTimeAllocator() {
		return this.timeAlloc;
	}

	public double evaluate() {
		return this.timeAlloc.evaluate(this.plannedActivities, this.initialDptTimesArray_s());
	}

	public BestTimeResponseTravelTimes getTravelTimes() {
		return this.myTravelTimes;
	}

	public double[] initialDptTimesArray_s() {
		final double[] result = new double[this.initialDptTimes_s.size()];
		for (int i = 0; i < this.initialDptTimes_s.size(); i++) {
			result[i] = this.initialDptTimes_s.get(i);
		}
		return result;
	}
}
