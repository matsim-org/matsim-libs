package org.matsim.contrib.roadpricing.run;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

public class RunRoadPricingExampleIT {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	private static final String TEST_CONFIG = "./test/input/org/matsim/contrib/roadpricing/AvoidTolledRouteTest/config.xml";

	@Test
	public void testRunToadPricingExample() {
		String[] args = new String[]{TEST_CONFIG
				, "--config:controler.outputDirectory=" + utils.getOutputDirectory()
		};
		try {
			RunRoadPricingExample.main(args);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Example should run without exceptions.");
		}
	}
}
