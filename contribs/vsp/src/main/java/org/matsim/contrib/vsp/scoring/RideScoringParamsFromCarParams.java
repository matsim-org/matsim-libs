package org.matsim.contrib.vsp.scoring;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.groups.ScoringConfigGroup;

public class RideScoringParamsFromCarParams {

	/**
	 * Sets ride scoring params based on car scoring params (including prices). This assumes that the ride passenger somehow has
	 * to compensate the driver for the additional driving effort to provide the ride.
	 *
	 * @param scoringConfigGroup config.scoring()
	 * @param alpha represents the share of an average ride trip that the car driver has to drive additionally to the car
	 *              trip to provide the ride. Typical values are between 1 (i.e. driver drives additional 10km to provide a 10 km
	 *              ride) and 2 (i.e. driver drives additional 20km to provide a 10 km ride). Alpha can be calibrated and must
	 *              be alpha >= 0.
	 */
	public static void setRideScoringParamsBasedOnCarParams (ScoringConfigGroup scoringConfigGroup, double alpha) {
		ScoringConfigGroup.ModeParams carParams = scoringConfigGroup.getOrCreateModeParams(TransportMode.car);
		ScoringConfigGroup.ModeParams rideParams = scoringConfigGroup.getOrCreateModeParams(TransportMode.ride);

		// constant is a calibration parameter and should not be changed

		// ride has no fixed cost
		rideParams.setDailyMonetaryConstant(0.0);

		// account for the driver's monetary distance rate.
		rideParams.setMonetaryDistanceRate(alpha * carParams.getMonetaryDistanceRate());

		// rider and driver have marginalUtilityOfDistance
		rideParams.setMarginalUtilityOfDistance((alpha + 1.0) * carParams.getMarginalUtilityOfDistance());

		// rider and driver have marginalUtilityOfTravelling, the driver additionally loses the opportunity to perform an activity
		rideParams.setMarginalUtilityOfTraveling((alpha + 1.0) * carParams.getMarginalUtilityOfTraveling() +
			alpha * -scoringConfigGroup.getPerforming_utils_hr());
	}
}
