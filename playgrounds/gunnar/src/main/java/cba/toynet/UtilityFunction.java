package cba.toynet;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

import com.google.inject.Provider;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class UtilityFunction {

	// -------------------- MEMBERS --------------------

	private final Scenario scenario;

	private final Map<TourSequence.Type, Double> tourSeq2ASC = new LinkedHashMap<>();

	private final TimeStructureOptimizer congTimeOpt;

	private final TimeStructureOptimizer telepTimeOpt;

	// -------------------- CONSTRUCTION --------------------

	UtilityFunction(final Scenario scenario, final Provider<TripRouter> tripRouterProvider,
			final Map<String, TravelTime> mode2travelTime, final int maxTrials, final int maxFailures,
			final boolean usePTto1, final boolean usePTto2, final double betaTravelSampers_1_h,
			final SampersCarDelay sampersCarDelay) {

		this.scenario = scenario;

		final double singleTourASC = 103.5;
		final double carASC = -1.30;
		final double pt1ASC = (usePTto1 ? 0.0 : Double.NEGATIVE_INFINITY);
		final double pt2ASC = (usePTto2 ? 0.0 : Double.NEGATIVE_INFINITY);

		this.tourSeq2ASC.put(TourSequence.Type.work_car, singleTourASC + carASC);
		this.tourSeq2ASC.put(TourSequence.Type.work_car_other1_car, 0.0 + carASC);
		this.tourSeq2ASC.put(TourSequence.Type.work_car_other1_pt, 0.0 + carASC + pt1ASC);
		this.tourSeq2ASC.put(TourSequence.Type.work_car_other2_car, 0.0 + carASC);
		this.tourSeq2ASC.put(TourSequence.Type.work_pt, singleTourASC + pt1ASC);
		this.tourSeq2ASC.put(TourSequence.Type.work_pt_other1_car, 0.0 + pt1ASC);
		this.tourSeq2ASC.put(TourSequence.Type.work_pt_other1_pt, 0.0 + pt1ASC);
		this.tourSeq2ASC.put(TourSequence.Type.work_pt_other2_car, 0.0 + pt1ASC);

		this.congTimeOpt = new TimeStructureOptimizer(TimeStructureOptimizer.LOGIC.matsim, this.scenario,
				tripRouterProvider, maxTrials, maxFailures, mode2travelTime, this.newFreeFlowTTs(),
				betaTravelSampers_1_h, sampersCarDelay);
		this.telepTimeOpt = new TimeStructureOptimizer(TimeStructureOptimizer.LOGIC.sampers, this.scenario,
				tripRouterProvider, maxTrials, maxFailures, mode2travelTime, this.newFreeFlowTTs(),
				betaTravelSampers_1_h, sampersCarDelay);
	}

	private Map<String, TravelTime> newFreeFlowTTs() {
		final Map<String, TravelTime> result = new LinkedHashMap<>();
		result.put("car", new FreeSpeedTravelTime());
		return result;
	}

	// -------------------- IMPLEMENTATION --------------------

	private Double congTravelTimeUtility = null;
	private Double teleportationTravelTimeUtility = null;
	private Double ActivityModeOnlyUtility = null;

	Double getCongestedTravelTimeUtility() {
		return this.congTravelTimeUtility;
	}

	Double getTeleportationTravelTimeUtility() {
		return this.teleportationTravelTimeUtility;
	}

	Double getActivityModeOnlyUtility() {
		return this.ActivityModeOnlyUtility;
	}

	void evaluate(final Plan plan, final TourSequence tourSeq) {
		this.teleportationTravelTimeUtility = this.telepTimeOpt.computeScoreAndSetDepartureTimes(plan, tourSeq);
		this.congTravelTimeUtility = this.congTimeOpt.computeScoreAndSetDepartureTimes(plan, tourSeq);
		this.ActivityModeOnlyUtility = this.tourSeq2ASC.get(tourSeq.type);
	}

	// TODO NEW
	// public String allUtilitiesToString(final Person person) {
	// final StringBuffer result = new StringBuffer("TYPE\tSAMPERS\tMATSim");
	// for (TourSequence.Type type : TourSequence.Type.values()) {
	// final TourSequence representativeTourSequence = new TourSequence(type);
	// final Plan representativePlan =
	// (representativeTourSequence).asPlan(this.scenario, person);
	// this.evaluate(representativePlan, representativeTourSequence);
	// result.append("\n");
	// result.append(type.toString());
	// result.append("\t");
	// result.append(this.teleportationTravelTimeUtility +
	// this.ActivityModeOnlyUtility);
	// result.append("\t");
	// result.append(this.congTravelTimeUtility + this.ActivityModeOnlyUtility);
	// }
	// return result.toString();
	// }
}
