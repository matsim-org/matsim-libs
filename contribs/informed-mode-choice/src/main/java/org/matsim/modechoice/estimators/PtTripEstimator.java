package org.matsim.modechoice.estimators;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.scoring.functions.ModeUtilityParameters;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.modechoice.EstimatorContext;
import org.matsim.modechoice.ModeAvailability;
import org.matsim.modechoice.PlanModel;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Estimates pt trip scores.
 * <p>
 * Based on the estimator in the discrete_mode_choice module.
 */
public class PtTripEstimator implements TripEstimator {

	private static final Logger log = LogManager.getLogger(PtTripEstimator.class);

	private final TransitSchedule transitSchedule;
	private final Map<Tuple<Id<TransitLine>, Id<TransitRoute>>, List<Double>> orderedDepartureTimes = new HashMap<>();

	@Inject
	public PtTripEstimator(TransitSchedule transitSchedule) {
		this.transitSchedule = transitSchedule;

		for (TransitLine transitLine : transitSchedule.getTransitLines().values()) {
			for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
				List<Double> departureTimes = transitRoute.getDepartures().values().stream()
						.map(Departure::getDepartureTime).sorted().collect(Collectors.toList());
				orderedDepartureTimes.put(createId(transitLine, transitRoute), departureTimes);
			}
		}
	}

	private static Tuple<Id<TransitLine>, Id<TransitRoute>> createId(TransitLine transitLine, TransitRoute transitRoute) {
		return new Tuple<>(transitLine.getId(), transitRoute.getId());
	}

	@Override
	public MinMaxEstimate estimate(EstimatorContext context, String mode, PlanModel plan, List<Leg> trip, ModeAvailability option) {

		ModeUtilityParameters params = context.scoring.modeParams.get(TransportMode.pt);

		double estimate = 0;
		int numberOfVehicularLegs = 0;
		double totalWaitingTime = 0.0;

		for (Leg leg : trip) {

			// Only score PT legs, other legs are estimated upstream
			if (TransportMode.pt.equals(leg.getMode())) {

				TransitPassengerRoute route = (TransitPassengerRoute) leg.getRoute();

				totalWaitingTime += estimateWaitingTime(leg.getDepartureTime().seconds(), route);
				numberOfVehicularLegs++;

				// Default leg scoring
				estimate += scoreLeg(leg, context, params);
			}

		}

		estimate += context.scoring.marginalUtilityOfWaitingPt_s * totalWaitingTime;

		if (numberOfVehicularLegs > 0) {
			estimate += context.scoring.utilityOfLineSwitch * (numberOfVehicularLegs - 1);
		}


		return MinMaxEstimate.ofMax(estimate);
	}

	protected double scoreLeg(Leg leg, EstimatorContext context, ModeUtilityParameters params) {
		double dist = leg.getRoute().getDistance();
		double tt = leg.getTravelTime().orElse(0);

		return params.constant +
				params.marginalUtilityOfDistance_m * dist +
				params.marginalUtilityOfTraveling_s * tt +
				context.scoring.marginalUtilityOfMoney * params.monetaryDistanceCostRate * dist;
	}

	protected double estimateWaitingTime(double agentDepartureTime, TransitPassengerRoute route) {
		TransitLine transitLine = transitSchedule.getTransitLines().get(route.getLineId());
		TransitRoute transitRoute = transitLine.getRoutes().get(route.getRouteId());

		List<Double> departureTimes = orderedDepartureTimes.get(createId(transitLine, transitRoute));

		List<Double> offsets = transitRoute.getStops().stream()
				.filter(stop -> stop.getStopFacility().getId().equals(route.getAccessStopId()))
				.map(TransitRouteStop::getDepartureOffset).map(OptionalTime::seconds).collect(Collectors.toList());

		double minimalWaitingTime = Double.POSITIVE_INFINITY;

		for (double routeDepartureTime : departureTimes) {
			for (double offset : offsets) {
				if (minimalWaitingTime == 0.0) {
					return 0.0;
				}

				double stopDepartureTime = routeDepartureTime + offset;

				if (stopDepartureTime >= agentDepartureTime) {
					double waitingTime = stopDepartureTime - agentDepartureTime;
					minimalWaitingTime = Math.min(minimalWaitingTime, waitingTime);
				}
			}
		}

		if (Double.isFinite(minimalWaitingTime)) {
			return minimalWaitingTime;
		} else {
			log.error(String.format(
					"Unable to find waiting time for departure on Line %s, Route %s, at Stop %s, after %s. Falling back to 0s.",
					transitLine.getId(), transitRoute.getId(), route.getAccessStopId(),
					Time.writeTime(agentDepartureTime)));
			return 0.0;
		}
	}
}
