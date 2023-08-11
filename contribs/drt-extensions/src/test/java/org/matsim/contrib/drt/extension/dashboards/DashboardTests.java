package org.matsim.contrib.drt.extension.dashboards;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.application.MATSimApplication;
import org.matsim.contrib.drt.extension.DrtTestScenario;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Path;

public class DashboardTests {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	private void run() {

		Config config = DrtTestScenario.loadConfig(utils);
		config.controler().setLastIteration(1);
		config.controler().setWritePlansInterval(2);

		SimWrapperConfigGroup group = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);
		group.defaultParams().sampleSize = Double.valueOf("0.001");
		group.defaultParams().mapCenter = "11.891000, 48.911000";

		MultiModeDrtConfigGroup multiModeDrtConfigGroup = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
		for (DrtConfigGroup drtCfg : multiModeDrtConfigGroup.getModalElements()) {
			if (drtCfg.getMode().equals("drt")){
				drtCfg.operationalScheme = DrtConfigGroup.OperationalScheme.serviceAreaBased;
				drtCfg.drtServiceAreaShapeFile = "drt-zones/drt-zonal-system.shp";
			}
		}

		Controler controler = MATSimApplication.prepare(new DrtTestScenario(config), config);
		controler.run();
	}

	@Test
	public void drtDefaults() {

		Path out = Path.of(utils.getOutputDirectory(), "analysis", "drt");

		run();

		// TODO: add assertions
	}
}
