package besttimeresponseintegration;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.router.ActivityWrapperFacility;
import org.matsim.core.scoring.functions.ActivityUtilityParameters;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.Facility;

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

	public final List<PlannedActivity<Facility, String>> plannedActivities;

	public final List<Double> initialDptTimes_s;

	private final TimeAllocator<Facility, String> timeAlloc;

	private final BestTimeResponseTravelTimes myTravelTimes;

	private final boolean verbose = false;
	
	// -------------------- CONSTRUCTION --------------------

	public BestTimeResponseStrategyFunctionality(final Plan plan, final Network network,
			final CharyparNagelScoringParametersForPerson scoringParams, final TimeDiscretization timeDiscretization,
			final BestTimeResponseTravelTimes myTravelTimes) {

		final CharyparNagelScoringParameters personScoringParams = scoringParams.getScoringParameters(plan.getPerson());

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
			final Leg matsimNextLeg = (Leg) plan.getPlanElements().get(q + 1);
			this.initialDptTimes_s.add(matsimAct.getEndTime());

			final ActivityUtilityParameters params = personScoringParams.utilParams.get(matsimAct.getType());

			final Double openingTime_s = ((params.getOpeningTime() == Time.UNDEFINED_TIME) ? null
					: params.getOpeningTime());
			final Double closingTime_s = ((params.getClosingTime() == Time.UNDEFINED_TIME) ? null
					: params.getClosingTime());
			final Double latestStartTime_s = ((params.getLatestStartTime() == Time.UNDEFINED_TIME) ? null
					: params.getLatestStartTime());
			final Double earliestEndTime_s = ((params.getEarliestEndTime() == Time.UNDEFINED_TIME) ? null
					: params.getEarliestEndTime());

			final double betaDur_1_s = personScoringParams.marginalUtilityOfPerforming_s;
			final double betaTravel_1_s = personScoringParams.modeParams
					.get(matsimNextLeg.getMode()).marginalUtilityOfTraveling_s;
			final double betaLateArr_1_s = personScoringParams.marginalUtilityOfLateArrival_s;
			final double betaEarlyDpt_1_s = personScoringParams.marginalUtilityOfEarlyDeparture_s;

			final PlannedActivity<Facility, String> plannedAct = new PlannedActivity<>(
					(Facility) new ActivityWrapperFacility(matsimAct), matsimNextLeg.getMode(),
					params.getTypicalDuration(), Units.S_PER_H * params.getZeroUtilityDuration_h(), openingTime_s,
					closingTime_s, latestStartTime_s, earliestEndTime_s, betaDur_1_s, betaEarlyDpt_1_s, betaLateArr_1_s,
					betaTravel_1_s);

			this.plannedActivities.add(plannedAct);
			
			if (this.verbose) {
				System.out.println(plannedAct);
			}
			
		}

		/*
		 * Evaluating the plan.
		 */

		final boolean repairTimeStructure = true;
		final boolean randomSmoothing = true;

		this.myTravelTimes = myTravelTimes;

		this.timeAlloc = new TimeAllocator<>(timeDiscretization, this.myTravelTimes,
				repairTimeStructure, randomSmoothing);
	}

	// -------------------- IMPLEMENTATION --------------------

	public TimeAllocator<Facility, String> getTimeAllocator() {
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
