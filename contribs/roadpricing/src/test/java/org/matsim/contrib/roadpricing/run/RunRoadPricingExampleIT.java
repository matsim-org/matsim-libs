package org.matsim.contrib.roadpricing.run;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.testcases.MatsimTestUtils;

public class RunRoadPricingExampleIT {
	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();

	private static final String TEST_CONFIG = "./test/input/org/matsim/contrib/roadpricing/AvoidTolledRouteTest/config.xml";

	@Test
	void testRunToadPricingExample() {
		String[] args = new String[]{TEST_CONFIG
				, "--config:controler.outputDirectory=" + utils.getOutputDirectory()
		};
		try {
			RunRoadPricingExample.main(args);
		} catch (Exception e) {
			e.printStackTrace();
			Assertions.fail("Example should run without exceptions.");
		}
	}
}
