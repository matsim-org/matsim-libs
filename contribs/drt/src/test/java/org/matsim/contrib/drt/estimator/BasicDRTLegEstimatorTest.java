package org.matsim.contrib.drt.estimator;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.application.MATSimApplication;
import org.matsim.contrib.drt.DrtTestScenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestUtils;

public class BasicDRTLegEstimatorTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	private Controler controler;

	@Before
	public void setUp() throws Exception {

		Config config = DrtTestScenario.loadConfig(utils);

		config.controler().setLastIteration(1);

		controler = MATSimApplication.prepare(DrtTestScenario.class, config);
	}

	@Test
	public void run() {

		// TODO

		controler.run();

	}
}
