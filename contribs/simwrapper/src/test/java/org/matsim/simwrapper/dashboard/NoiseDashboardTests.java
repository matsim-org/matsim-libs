package org.matsim.simwrapper.dashboard;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.simwrapper.SimWrapper;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.simwrapper.TestScenario;
import org.matsim.testcases.MatsimTestUtils;


import java.net.URL;

public class NoiseDashboardTests {


	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void generate() {
		Config config = TestScenario.loadConfig(utils);

		config.global().setCoordinateSystem("EPSG:25832");

		SimWrapperConfigGroup simWrapperConfigGroup = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);

		URL kelheim = ExamplesUtils.getTestScenarioURL("kelheim");

		simWrapperConfigGroup.defaultParams().shp = IOUtils.extendUrl(kelheim, "area/area.shp").toString();


		SimWrapper sw = SimWrapper.create(config).addDashboard(new NoiseDashboard());
		Controler controler = MATSimApplication.prepare(new TestScenario(sw), config);


		controler.run();
	}
}
