package org.matsim.contrib.vsp.scoring;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;

public class RideScoringParamsFromCarParamsTest {

	@Test
	void testSetRideScoringParamsBasedOnCarParams() {
		Config config = ConfigUtils.createConfig();
		ScoringConfigGroup scoringConfigGroup = config.scoring();

		scoringConfigGroup.setPerforming_utils_hr(6.0);
		ScoringConfigGroup.ModeParams carParams = scoringConfigGroup.getOrCreateModeParams(TransportMode.car);
		carParams.setDailyMonetaryConstant(-10.0);
		carParams.setMarginalUtilityOfDistance(-2.0);
		carParams.setMarginalUtilityOfTraveling(-1.0);
		carParams.setMonetaryDistanceRate(-0.3);

		double alpha = 2.0;

		RideScoringParamsFromCarParams.setRideScoringParamsBasedOnCarParams(scoringConfigGroup, alpha);

		ScoringConfigGroup.ModeParams rideParams = scoringConfigGroup.getOrCreateModeParams(TransportMode.ride);
		Assertions.assertEquals(0.0, rideParams.getDailyMonetaryConstant());
		Assertions.assertEquals(-0.6, rideParams.getMonetaryDistanceRate());
		Assertions.assertEquals(-6.0, rideParams.getMarginalUtilityOfDistance());
		Assertions.assertEquals(alpha * -6.0 + (alpha + 1) * -1.0, rideParams.getMarginalUtilityOfTraveling());
	}
}
