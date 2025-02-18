package org.matsim.contrib.vsp.pt.fare;

import com.google.inject.Inject;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleDoublePair;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.scoring.functions.ModeUtilityParameters;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.modechoice.EstimatorContext;
import org.matsim.modechoice.ModeAvailability;
import org.matsim.modechoice.PlanModel;
import org.matsim.modechoice.estimators.MinMaxEstimate;
import org.matsim.modechoice.estimators.PtTripEstimator;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.List;
import java.util.Map;

public class PtTripWithDistanceBasedFareEstimator extends PtTripEstimator {

	private final PtFareConfigGroup config;
	private final DistanceBasedPtFareParams ptFare;
	private final Map<Id<TransitStopFacility>, TransitStopFacility> facilities;

	@Inject
	public PtTripWithDistanceBasedFareEstimator(TransitSchedule transitSchedule, PtFareConfigGroup config,
												Scenario scenario) {
		super(transitSchedule);
		this.config = config;
		this.ptFare = extractPtFare(config);
		this.facilities = scenario.getTransitSchedule().getFacilities();
	}

	private static DistanceBasedPtFareParams extractPtFare(PtFareConfigGroup config) {
		//TODO
		return config.getParameterSets(DistanceBasedPtFareParams.SET_TYPE).stream()
					 .map(DistanceBasedPtFareParams.class::cast)
					 .findFirst()
					 .orElseThrow(() -> new IllegalStateException("No distance based fare parameters found"));
	}

	@Override
	public MinMaxEstimate estimate(EstimatorContext context, String mode, PlanModel plan, List<Leg> trip, ModeAvailability option) {

		DoubleDoublePair p = estimateTrip(context, trip);

		double estimate = p.leftDouble();
		double fareUtility = p.rightDouble();

		double max;
		if (providesMinEstimate(context, mode, option)) {
			double maxFareUtility = -context.scoring.marginalUtilityOfMoney * calcMinimumFare(plan);

			// It can happen that this bound is not even better
			// this is the case if there is one trip only
			// or for instance if there is one short and one very long trip
			// the upper bound might have no effect in these cases
			maxFareUtility = Math.max(fareUtility, maxFareUtility);

			max = estimate + maxFareUtility;
		} else {
			max = estimate + fareUtility;
		}

		// Distance fareUtility is the highest possible price, therefore the minimum utility
		return MinMaxEstimate.of(estimate + fareUtility, max);
	}

	@Override
	@SuppressWarnings("StringEquality")
	public double estimatePlan(EstimatorContext context, String mode, String[] modes, PlanModel plan, ModeAvailability option) {

		double utility = 0;
		DoubleList fares = new DoubleArrayList();

		assert plan.trips() == modes.length : String.format("Plan and mode length differ: %d vs. %d", plan.trips(), mode.length());

		for (int i = 0; i < modes.length; i++) {

			if (modes[i] == TransportMode.pt) {

				List<Leg> legs = plan.getLegs(mode, i);

				// Legs can be null if there is a predefined pt trip
				if (legs == null) {
					continue;
				}

				//assert legs != null : "Legs must be not null at this point";

				DoubleDoublePair p = estimateTrip(context, legs);

				utility += p.firstDouble();
				fares.add(p.rightDouble());
			}
		}

		// fares are negative, so the minimum is used
		double maxFare = fares.doubleStream().min().orElse(0);
		double sumFares = fares.doubleStream().sum();

		return utility + Math.max(maxFare * config.getUpperBoundFactor(), sumFares);
	}

	/**
	 * Estimate utility and fare for one trip.
	 *
	 * @return pair of utility and maximum distance based fare
	 */
	private DoubleDoublePair estimateTrip(EstimatorContext context, List<Leg> trip) {

		ModeUtilityParameters params = context.scoring.modeParams.get(TransportMode.pt);

		double estimate = 0;
		int numberOfVehicularLegs = 0;
		double totalWaitingTime = 0.0;

		// first access and last egress
		TransitStopFacility access = null;
		TransitStopFacility egress = null;

		for (Leg leg : trip) {

			// Only score PT legs, other legs are estimated upstream
			if (TransportMode.pt.equals(leg.getMode())) {

				TransitPassengerRoute route = (TransitPassengerRoute) leg.getRoute();

				if (access == null) {
					access = facilities.get(route.getAccessStopId());
				}

				egress = facilities.get(route.getEgressStopId());

				totalWaitingTime += estimateWaitingTime(leg.getDepartureTime().seconds(), route);
				numberOfVehicularLegs++;

				// Default leg scoring
				estimate += scoreLeg(leg, context, params);
			}

		}

		assert access != null && egress != null : "At least one PT trip must be present";

		double dist = CoordUtils.calcEuclideanDistance(access.getCoord(), egress.getCoord());

		double fareUtility = -context.scoring.marginalUtilityOfMoney * DistanceBasedPtFareCalculator.computeFare(dist,
			ptFare.getMinFare(), ptFare.getDistanceClassFareParams());

		estimate += context.scoring.marginalUtilityOfWaitingPt_s * totalWaitingTime;

		if (numberOfVehicularLegs > 0) {
			estimate += context.scoring.utilityOfLineSwitch * (numberOfVehicularLegs - 1);
		}

		return DoubleDoublePair.of(estimate, fareUtility);
	}

	/**
	 * The minimum fare a person would have to pay for an avg. trip
	 */
	private double calcMinimumFare(PlanModel plan) {

		double minDist = Double.POSITIVE_INFINITY;
		double secondMinDist = Double.POSITIVE_INFINITY;

		int n = 0;
		for (int i = 0; i < plan.trips(); i++) {

			List<Leg> legs = plan.getLegs(TransportMode.pt, i);

			if (legs == null) {
				continue;
			}

			// first access and last egress
			TransitStopFacility access = null;
			TransitStopFacility egress = null;

			boolean hasPT = false;

			// only the distances are important
			for (Leg leg : legs) {

				// Only score PT legs, other legs are estimated upstream
				if (TransportMode.pt.equals(leg.getMode())) {

					TransitPassengerRoute route = (TransitPassengerRoute) leg.getRoute();

					hasPT = true;
					if (access == null) {
						access = facilities.get(route.getAccessStopId());
					}

					egress = facilities.get(route.getEgressStopId());
				}
			}

			if (hasPT) {
				double dist = CoordUtils.calcEuclideanDistance(access.getCoord(), egress.getCoord());

				if (dist < minDist) {
					minDist = dist;
					secondMinDist = minDist;
				} else if (dist < secondMinDist) {
					secondMinDist = dist;
				}

				n++;
			}
		}

		// a single pt trip could never benefit from the upper bound
		if (n == 1) {
			return DistanceBasedPtFareCalculator.computeFare(minDist, ptFare.getMinFare(), ptFare.getDistanceClassFareParams());
		}

		// the upper bound is the maximum single trip times a factor
		// therefore the minimum upper bound is the fare for the second-longest trip
		// the max costs are then assumed to be evenly distributed over all pt trips
		return 1d / n * config.getUpperBoundFactor() * DistanceBasedPtFareCalculator.computeFare(secondMinDist,
			ptFare.getMinFare(), ptFare.getDistanceClassFareParams());
	}

	@Override
	public boolean providesMinEstimate(EstimatorContext context, String mode, ModeAvailability option) {
		return config.getApplyUpperBound();
	}
}
