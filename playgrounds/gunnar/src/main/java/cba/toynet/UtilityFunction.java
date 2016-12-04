package cba.toynet;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.util.TravelTime;

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
			final Map<String, TravelTime> mode2travelTime, final int maxTrials, final int maxFailures) {

		this.scenario = scenario;

		this.tourSeq2ASC.put(TourSequence.Type.work_car, 103.5);
		this.tourSeq2ASC.put(TourSequence.Type.work_car_other1_car, 0.0);
		this.tourSeq2ASC.put(TourSequence.Type.work_car_other1_pt, 0.0);
		this.tourSeq2ASC.put(TourSequence.Type.work_car_other2_car, 0.0);
		this.tourSeq2ASC.put(TourSequence.Type.work_pt, 103.5);
		this.tourSeq2ASC.put(TourSequence.Type.work_pt_other1_car, 0.0);
		this.tourSeq2ASC.put(TourSequence.Type.work_pt_other1_pt, 0.0);
		this.tourSeq2ASC.put(TourSequence.Type.work_pt_other2_car, 0.0);

		this.congTimeOpt = new TimeStructureOptimizer(this.scenario, tripRouterProvider, maxTrials, maxFailures,
				mode2travelTime);
		this.telepTimeOpt = new TimeStructureOptimizer(this.scenario, null, maxTrials, maxFailures, null);
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
		this.teleportationTravelTimeUtility = this.telepTimeOpt.computeScoreAndSetDepartureTimes(plan);
		this.congTravelTimeUtility = this.congTimeOpt.computeScoreAndSetDepartureTimes(plan);
		this.ActivityModeOnlyUtility = this.tourSeq2ASC.get(tourSeq.type);
	}
}
