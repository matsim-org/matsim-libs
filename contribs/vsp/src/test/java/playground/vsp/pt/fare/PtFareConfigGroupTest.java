package playground.vsp.pt.fare;

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
		fareZoneBased.setPriority(5);
		ptFareConfigGroup.addParameterSet(fareZoneBased);

		DistanceBasedPtFareParams distanceBased = new DistanceBasedPtFareParams();
		distanceBased.setPriority(5);
		ptFareConfigGroup.addParameterSet(distanceBased);

		Assertions.assertThrows(IllegalArgumentException.class, () -> ptFareConfigGroup.checkConsistency(config));
	}

	@Test
	void test_ok() {
		Config config = ConfigUtils.createConfig();
		PtFareConfigGroup ptFareConfigGroup = ConfigUtils.addOrGetModule(config, PtFareConfigGroup.class);
		FareZoneBasedPtFareParams fareZoneBased = new FareZoneBasedPtFareParams();
		fareZoneBased.setPriority(5);
		ptFareConfigGroup.addParameterSet(fareZoneBased);

		DistanceBasedPtFareParams distanceBased = new DistanceBasedPtFareParams();
		distanceBased.setPriority(10);
		ptFareConfigGroup.addParameterSet(distanceBased);

		ptFareConfigGroup.checkConsistency(config);
	}
}
