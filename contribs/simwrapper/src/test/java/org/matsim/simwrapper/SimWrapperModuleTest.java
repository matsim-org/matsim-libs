package org.matsim.simwrapper;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.application.analysis.traffic.LinkStats;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.simwrapper.viz.PieChart;
import org.matsim.simwrapper.viz.TextBlock;
import org.matsim.testcases.MatsimTestUtils;

import java.net.URL;

public class SimWrapperModuleTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void runScenario() {

		URL equil = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml");

		Config config = ConfigUtils.loadConfig(equil);

		config.controler().setLastIteration(5);
		config.controler().setOutputDirectory(utils.getOutputDirectory());

		Controler controler = new Controler(config);
		controler.addOverridingModule(new SimWrapperModule());
		controler.run();

	}
}
