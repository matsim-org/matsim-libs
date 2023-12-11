package org.matsim.contrib.roadpricing.run;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RunRoadPricingUsingTollFactorExampleIT {
	private static final String TEST_CONFIG = "./test/input/org/matsim/contrib/roadpricing/AvoidTolledRouteTest/config.xml";

	@Test
	void testRunRoadPRicingUsingTollFactorExample() {
		String[] args = new String[]{TEST_CONFIG};
		try {
			RunRoadPricingUsingTollFactorExample.main(args);
		} catch (Exception e) {
			e.printStackTrace();
			Assertions.fail("Example should run without exceptions.");
		}
	}
}