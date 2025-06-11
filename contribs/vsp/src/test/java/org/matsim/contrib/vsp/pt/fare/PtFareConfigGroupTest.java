package org.matsim.contrib.vsp.pt.fare;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

class PtFareConfigGroupTest {
	@Test
	void testNoFareParams_throws() {
		Config config = ConfigUtils.createConfig();
		PtFareConfigGroup ptFareConfigGroup = ConfigUtils.addOrGetModule(config, PtFareConfigGroup.class);
		Assertions.assertThrows(IllegalArgumentException.class, () -> ptFareConfigGroup.checkConsistency(config));
	}

	@Test
	void testSamePriority_throws() {
		Config config = ConfigUtils.createConfig();
		PtFareConfigGroup ptFareConfigGroup = ConfigUtils.addOrGetModule(config, PtFareConfigGroup.class);
		FareZoneBasedPtFareParams fareZoneBased = new FareZoneBasedPtFareParams();
		fareZoneBased.setOrder(5);
		ptFareConfigGroup.addPtFareParameterSet(fareZoneBased);

		DistanceBasedPtFareParams distanceBased = new DistanceBasedPtFareParams();
		distanceBased.setOrder(5);
		ptFareConfigGroup.addPtFareParameterSet(distanceBased);

		Assertions.assertThrows(IllegalArgumentException.class, () -> ptFareConfigGroup.checkConsistency(config));
	}

	@Test
	void test_ok() {
		Config config = ConfigUtils.createConfig();
		PtFareConfigGroup ptFareConfigGroup = ConfigUtils.addOrGetModule(config, PtFareConfigGroup.class);
		FareZoneBasedPtFareParams fareZoneBased = new FareZoneBasedPtFareParams();
		fareZoneBased.setOrder(5);
		ptFareConfigGroup.addPtFareParameterSet(fareZoneBased);

		DistanceBasedPtFareParams distanceBased = new DistanceBasedPtFareParams();
		distanceBased.setOrder(10);
		ptFareConfigGroup.addPtFareParameterSet(distanceBased);

		ptFareConfigGroup.checkConsistency(config);
	}
}
