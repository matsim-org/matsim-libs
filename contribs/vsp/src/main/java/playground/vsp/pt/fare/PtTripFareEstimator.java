package playground.vsp.pt.fare;

import com.google.inject.Inject;
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

public class PtTripFareEstimator extends PtTripEstimator {

	private final PtFareConfigGroup config;
	private final DistanceBasedPtFareParams ptFare;

	private final Map<Id<TransitStopFacility>, TransitStopFacility> facilities;

	@Inject
	public PtTripFareEstimator(TransitSchedule transitSchedule, PtFareConfigGroup config, DistanceBasedPtFareParams ptFare, Scenario scenario) {
		super(transitSchedule);
		this.config = config;
		this.ptFare = ptFare;
		this.facilities = scenario.getTransitSchedule().getFacilities();
	}

	@Override
	public MinMaxEstimate estimate(EstimatorContext context, String mode, PlanModel plan, List<Leg> trip, ModeAvailability option) {

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

				if (access == null)
					access = facilities.get(route.getAccessStopId());

				egress = facilities.get(route.getEgressStopId());

				totalWaitingTime += estimateWaitingTime(leg.getDepartureTime().seconds(), route);
				numberOfVehicularLegs++;

				// Default leg scoring
				estimate += scoreLeg(leg, context, params);
			}

		}

		assert access != null && egress != null : "At least one PT trip must be present";

		double dist = CoordUtils.calcEuclideanDistance(access.getCoord(), egress.getCoord());

		double fareUtility = -context.scoring.marginalUtilityOfMoney * DistanceBasedPtFareHandler.computeFare(dist, ptFare.getLongDistanceTripThreshold(), ptFare.getMinFare(),
				ptFare.getNormalTripIntercept(), ptFare.getNormalTripSlope(), ptFare.getLongDistanceTripIntercept(), ptFare.getLongDistanceTripSlope());


		estimate += context.scoring.marginalUtilityOfWaitingPt_s * totalWaitingTime;

		if (numberOfVehicularLegs > 0) {
			estimate += context.scoring.utilityOfLineSwitch * (numberOfVehicularLegs - 1);
		}

		double max = estimate;
		if (providesMaxEstimate(context, mode, option)) {
			double maxFareUtility = -context.scoring.marginalUtilityOfMoney * calcMinimumFare(plan);

			// It can happen that this bound is not even better
			// this is the case if there is one trip only
			// or for instance if there is one short and one very long trip
			// the upper bound might have no effect in these cases
			maxFareUtility = Math.max(fareUtility, maxFareUtility);

			max = estimate + maxFareUtility;
		}

		return MinMaxEstimate.of(estimate + fareUtility, max);
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

			if (legs == null)
				continue;

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
					if (access == null)
						access = facilities.get(route.getAccessStopId());

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
			return DistanceBasedPtFareHandler.computeFare(minDist, ptFare.getLongDistanceTripThreshold(), ptFare.getMinFare(),
					ptFare.getNormalTripIntercept(), ptFare.getNormalTripSlope(), ptFare.getLongDistanceTripIntercept(), ptFare.getLongDistanceTripSlope());
		}

		// the upper bound is the maximum single trip times a factor
		// therefore the minimum upper bound is the fare for the second-longest trip
		// the max costs are then assumed to be evenly distributed over all pt trips
		return 1d/n * config.getUpperBoundFactor() * DistanceBasedPtFareHandler.computeFare(secondMinDist, ptFare.getLongDistanceTripThreshold(), ptFare.getMinFare(),
				ptFare.getNormalTripIntercept(), ptFare.getNormalTripSlope(), ptFare.getLongDistanceTripIntercept(), ptFare.getLongDistanceTripSlope());
	}

	@Override
	public boolean providesMaxEstimate(EstimatorContext context, String mode, ModeAvailability option) {
		return config.getApplyUpperBound();
	}
}
