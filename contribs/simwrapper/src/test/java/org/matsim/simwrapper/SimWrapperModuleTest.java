package org.matsim.simwrapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.net.URL;

public class SimWrapperModuleTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void runScenario_bindOnlyWithSpi() {
		URL equil = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml");

		Config config = ConfigUtils.loadConfig(equil);

		config.controller().setLastIteration(5);
		config.controller().setOutputDirectory(utils.getOutputDirectory());

		Controler controler = new Controler(config);
		controler.addOverridingModule(new SimWrapperModule());
		controler.run();

		//default ones + TestDashboard
		assertDashboardFiles(5);
	}

	@Test
	void runWithTestDashboard_bindAlsoWithGuice() {
		URL equil = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml");

		Config config = ConfigUtils.loadConfig(equil);

		config.controller().setLastIteration(0);
		config.controller().setOutputDirectory(utils.getOutputDirectory());

		Controler controler = new Controler(config);
		controler.addOverridingModule(new SimWrapperModule());
		TestDashboardProvider testDashboard = new TestDashboardProvider();
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				SimWrapper.addDashboardProviderBinding(binder()).toInstance(testDashboard);
			}
		});

		controler.run();

		//default ones + test dashboard
		assertDashboardFiles(5);
		testDashboard.assertCalled();
	}

	@Test
	void runWithoutDefault_bindAlsoWithGuice() {
		URL equil = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml");

		Config config = ConfigUtils.loadConfig(equil);

		config.controller().setLastIteration(0);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class).setDefaultDashboards(SimWrapperConfigGroup.Mode.disabled);

		Controler controler = new Controler(config);
		controler.addOverridingModule(new SimWrapperModule());
		TestDashboardProvider testDashboard = new TestDashboardProvider();
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				SimWrapper.addDashboardProviderBinding(binder()).toInstance(testDashboard);
			}
		});

		controler.run();

		assertDashboardFiles(1);
		testDashboard.assertCalled();
	}

	@Test
	void runWithTestDashboard_bindOnlyWithSpi() {
		URL equil = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml");

		Config config = ConfigUtils.loadConfig(equil);

		config.controller().setLastIteration(0);
		config.controller().setOutputDirectory(utils.getOutputDirectory());

		Controler controler = new Controler(config);
		controler.addOverridingModule(new SimWrapperModule());
		controler.run();

		assertDashboardFiles(5);
	}

	@Test
	void runWithoutDefault_bindOnlyWithSpi() {
		URL equil = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml");

		Config config = ConfigUtils.loadConfig(equil);

		config.controller().setLastIteration(0);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		SimWrapperConfigGroup simWrapperConfigGroup = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);
		simWrapperConfigGroup.setDefaultDashboards(SimWrapperConfigGroup.Mode.disabled);

		Controler controler = new Controler(config);
		controler.addOverridingModule(new SimWrapperModule());
		controler.run();

		assertDashboardFiles(1);
	}

	private void assertDashboardFiles(int fileCount) {
		java.nio.file.Path outputDir = java.nio.file.Paths.get(utils.getOutputDirectory());
		try (java.util.stream.Stream<java.nio.file.Path> files = java.nio.file.Files.list(outputDir)) {
			java.util.List<String> dashboardFiles = files
				.map(p -> p.getFileName().toString())
				.filter(name -> name.startsWith("dashboard-") && name.endsWith(".yaml"))
				.sorted()
				.toList();

			Assertions.assertEquals(fileCount, dashboardFiles.size());
		} catch (java.io.IOException e) {
			Assertions.fail("Failed to list output directory files", e);
		}
	}
}
