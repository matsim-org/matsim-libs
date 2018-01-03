package org.matsim.contrib.minibus;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

public class RunMinibusExampleRaptor {
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
    public final void testRunScenarioWithRaptor() {
		String[] args = {"test/input/org/matsim/contrib/minibus/example-scenario/config_raptor.xml"};
//		RunMinibus.main(args);
	}

}
