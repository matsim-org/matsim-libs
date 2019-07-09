package org.matsim.contrib.roadpricing.run;

import org.junit.Assert;
import org.junit.Test;

public class RunRoadPricingUsingTollFactorExampleIT {
	private static final String TEST_CONFIG = "./test/input/org/matsim/contrib/roadpricing/AvoidTolledRouteTest/config.xml";

	@Test
	public void testRunRoadPRicingUsingTollFactorExample() {
		String[] args = new String[]{TEST_CONFIG};
		try {
			RunRoadPricingUsingTollFactorExample.main(args);
		} catch (Exception e) {
			Assert.fail("Example should run without exceptions.");
		}
	}
}