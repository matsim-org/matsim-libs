package org.matsim.contrib.roadpricing.run;

import org.junit.Assert;
import org.junit.Test;

public class RunRoadPricingExampleIT {
	private static final String TEST_CONFIG = "./test/input/org/matsim/contrib/roadpricing/AvoidTolledRouteTest/config.xml";

	@Test
	public void testRunToadPricingExample() {
		String[] args = new String[]{TEST_CONFIG};
		try {
			RunRoadPricingExample.main(args);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Example should run without exceptions.");
		}
	}
}