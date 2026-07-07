package org.matsim.core.scoring.functions;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PassengerRoute;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scoring.EventsToLegs;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Variant of leg scoring that first tries to resolve the scoring parameters via vehicle type
 * and only falls back to the leg mode if no vehicle-based parameters are defined.
 */
public class VehicleTypeBasedLegScoring implements SumScoringFunction.TripScoring {

	private final ScoringParameters params;
	private final double marginalUtilityOfMoney;
	private final Set<String> ptModes;
	private final Set<String> modesAlreadyConsideredForDailyConstants;
	private final DoubleList legScores;
	private final Vehicles vehicles;

	private double score;

	public VehicleTypeBasedLegScoring(Vehicles vehicles, ScoringParameters params, Set<String> ptModes) {
		this.vehicles = vehicles;
		this.params = params;
		this.marginalUtilityOfMoney = params.marginalUtilityOfMoney;
		this.ptModes = ptModes;
		this.modesAlreadyConsideredForDailyConstants = new HashSet<>();
		this.legScores = new DoubleArrayList();
	}

	public VehicleTypeBasedLegScoring(ScoringParameters params, Vehicles vehicles) {
		this(vehicles, params, new HashSet<>(Collections.singletonList("pt")));
	}

	@Override
	public void finish() {
	}

	@Override
	public double getScore() {
		return score;
	}

	@Override
	public void explainScore(StringBuilder out) {
		out.append("legs_util=").append(score);
		if (!legScores.isEmpty()) {
			for (int i = 0; i < legScores.size(); i++) {
				out.append(ScoringFunction.SCORE_DELIMITER).append(" legs_").append(i).append("_util=").append(legScores.getDouble(i));
			}
		}
	}

	@Override
	public void handleTrip(TripStructureUtils.Trip trip) {
		var seenModesInTrip = new HashSet<String>();
		var tripScore = 0.;
		var ptLegsInTrip = new AtomicInteger();

		for (var leg : trip.getLegsOnly()) {
			var timeScore = calcTravelTimeScore(leg);
			var distScore = calcTravelDistScore(leg);
			var waitScore = calcWaitScore(leg);
			var tripConstant = calcTripConstant(leg, seenModesInTrip);
			var dailyConstant = calcDailyConstant(leg, modesAlreadyConsideredForDailyConstants);
			var lineSwitch = calcLineSwitch(leg, ptLegsInTrip);
			var legScore = timeScore + distScore + waitScore + tripConstant + dailyConstant + lineSwitch;
			legScores.add(legScore);
			tripScore += legScore;
		}
		this.score += tripScore;
	}

	private double calcTravelTimeScore(Leg leg) {
		Gbl.assertIf(leg.getTravelTime().isDefined());
		return leg.getTravelTime().seconds() * getModeParams(leg).marginalUtilityOfTraveling_s;
	}

	private double calcTravelDistScore(Leg leg) {
		var modeParams = getModeParams(leg);
		if (modeParams.marginalUtilityOfDistance_m != 0 || modeParams.monetaryDistanceCostRate != 0) {
			if (Double.isNaN(leg.getRoute().getDistance())) {
				throw new RuntimeException("Distance is NaN which cannot be interpreted.");
			}
			var utilDist = leg.getRoute().getDistance() * modeParams.marginalUtilityOfDistance_m;
			var utilDistCosts = leg.getRoute().getDistance() * modeParams.monetaryDistanceCostRate * this.marginalUtilityOfMoney;
			return utilDist + utilDistCosts;
		}
		return 0.;
	}

	private double calcWaitScore(Leg leg) {
		if (leg.getRoute() instanceof PassengerRoute pr) {
			var waitTime = pr.getBoardingTime().seconds() - leg.getDepartureTime().seconds();
			var waitUtil = params.marginalUtilityOfWaitingPt_s - getModeParams(leg).marginalUtilityOfTraveling_s;
			return waitTime * waitUtil;
		}
		return 0.;
	}

	private double calcTripConstant(Leg leg, Set<String> seenModes) {
		String scoringKey = getScoringKey(leg);
		if (seenModes.contains(scoringKey)) {
			return 0.;
		}
		seenModes.add(scoringKey);
		return getModeParams(leg).constant;
	}

	private double calcDailyConstant(Leg leg, Set<String> seenModes) {
		String scoringKey = getScoringKey(leg);
		if (seenModes.contains(scoringKey)) {
			return 0.;
		}
		seenModes.add(scoringKey);
		var modeParams = getModeParams(leg);
		return modeParams.dailyUtilityConstant + modeParams.dailyMoneyConstant * params.marginalUtilityOfMoney;
	}

	private double calcLineSwitch(Leg leg, AtomicInteger ptLegsInTrip) {
		if (ptModes.contains(leg.getMode())) {
			if (ptLegsInTrip.incrementAndGet() > 1) {
				return params.utilityOfLineSwitch;
			}
		}
		return 0.;
	}

	private ModeUtilityParameters getModeParams(Leg leg) {
		return params.modeParams.computeIfAbsent(getScoringKey(leg), this::getFallbackModeParams);
	}

	private String getScoringKey(Leg leg) {
		Id<Vehicle> vehicleId = (Id<Vehicle>) leg.getAttributes().getAttribute(EventsToLegs.VEHICLE_ID_ATTRIBUTE_NAME);
		if (vehicleId != null && vehicles.getVehicles().containsKey(vehicleId)) {
			VehicleType vehicleType = vehicles.getVehicles().get(vehicleId).getType();
			if (vehicleType != null) {
				if (params.modeParams.containsKey(vehicleType.getId().toString())) {
					return vehicleType.getId().toString();
				}
				else {
					var thisModeParams = VehicleTypeBasedScoringUtils.createModeParams(vehicleType);
					params.modeParams.put(vehicleType.getId().toString(), new ModeUtilityParameters.Builder(thisModeParams).build());
					return vehicleType.getId().toString();
				}
			}
		}
		return leg.getMode();
	}

	private ModeUtilityParameters getFallbackModeParams(String key) {
		if (params.modeParams.containsKey(key)) {
			return params.modeParams.get(key);
		}
		if (key.equals(TransportMode.transit_walk) || key.equals(TransportMode.non_network_walk)) {
			return params.modeParams.get(TransportMode.walk);
		}
		throw new IllegalStateException("No scoring parameters defined for key " + key);
	}
}
