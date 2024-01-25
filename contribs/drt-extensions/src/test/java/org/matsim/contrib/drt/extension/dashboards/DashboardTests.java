package org.matsim.contrib.drt.extension.dashboards;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
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

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	private void run() {

		Config config = DrtTestScenario.loadConfig(utils);
		config.controller().setLastIteration(4);
		config.controller().setWritePlansInterval(4);
		config.controller().setWriteEventsInterval(4);

		SimWrapperConfigGroup group = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);
		group.sampleSize = 0.001;
		group.defaultParams().mapCenter = "11.891000, 48.911000";

		//we have 2 operators ('av' + 'drt'), configure one of them to be areaBased (the other remains stopBased)
		MultiModeDrtConfigGroup multiModeDrtConfigGroup = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
		for (DrtConfigGroup drtCfg : multiModeDrtConfigGroup.getModalElements()) {
			if (drtCfg.getMode().equals("av")){
				drtCfg.operationalScheme = DrtConfigGroup.OperationalScheme.serviceAreaBased;
				drtCfg.drtServiceAreaShapeFile = "drt-zones/drt-zonal-system.shp";
			}
		}

		Controler controler = MATSimApplication.prepare(new DrtTestScenario(config), config);
		controler.run();
	}

	@Test
	void drtDefaults() {
		run();

		// TODO: add test headers!?

		Path drtOutputPath = Path.of(utils.getOutputDirectory(), "analysis", "drt");
		{
			//test general output files
			Assertions.assertThat(drtOutputPath)
				.isDirectoryContaining("glob:**supply_kpi.csv");
			Assertions.assertThat(drtOutputPath)
				.isDirectoryContaining("glob:**demand_kpi.csv");
		}
		{
			//test output files specific for stopBased services
			Assertions.assertThat(drtOutputPath)
				.isDirectoryContaining("glob:**stops.shp");
			Assertions.assertThat(drtOutputPath)
				.isDirectoryContaining("glob:**trips_per_stop.csv");
			Assertions.assertThat(drtOutputPath)
				.isDirectoryContaining("glob:**trips_per_stop.csv");
		}

		Path avOutputPath = Path.of(utils.getOutputDirectory(), "analysis", "drt-av");
		{
			//test general output files
			Assertions.assertThat(avOutputPath)
				.isDirectoryContaining("glob:**supply_kpi.csv");
			Assertions.assertThat(avOutputPath)
				.isDirectoryContaining("glob:**demand_kpi.csv");
		}

		{
			//test output files specific for stopBased services
			Assertions.assertThat(avOutputPath)
				.isDirectoryContaining("glob:**serviceArea.shp");
		}
	}

}
