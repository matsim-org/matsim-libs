package org.matsim.simwrapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controller;
import org.matsim.core.controler.ControllerUtils;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimWrapperListenerTest {
	@RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void test() {
		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml"));
		config.controller().setOutputDirectory(utils.getOutputDirectory());

		Controller controller = ControllerUtils.createController(ScenarioUtils.loadScenario(config));
		MatsimServices services = controller.getInjector().getInstance(MatsimServices.class);

		SimWrapperListener simWrapperListener = new SimWrapperListener(SimWrapper.create(config), Set.of(), config, Set.of());

		simWrapperListener.notifyStartup(new StartupEvent(services));

	}

	@Test
	void testWithGuice() {
		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml"));
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		SimWrapperConfigGroup simWrapperConfigGroup = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);
		simWrapperConfigGroup.setDefaultDashboards(SimWrapperConfigGroup.Mode.disabled);

		Controller controller = ControllerUtils.createController(ScenarioUtils.loadScenario(config));

		TestDashboardProvider testDashboardProvider = new TestDashboardProvider();

		MatsimServices services = controller.getInjector().getInstance(MatsimServices.class);

		SimWrapperListener simWrapperListener = new SimWrapperListener(SimWrapper.create(config), Set.of(), config, Set.of(testDashboardProvider));
		simWrapperListener.notifyStartup(new StartupEvent(services));

		testDashboardProvider.assertCalled();
	}

	@Test
	void testServiceLoaderFindsTestProvider() {
		ServiceLoader<DashboardProvider> loader = ServiceLoader.load(DashboardProvider.class);
		List<DashboardProvider> found = StreamSupport.stream(loader.spliterator(), false).toList();

		assertEquals(2, found.size(), "No DashboardProvider found via SPI");
		assertTrue(found.getFirst() instanceof TestDashboardProvider, "TestDashboardProvider should be loaded via SPI");
		assertTrue(found.getLast() instanceof DefaultDashboardProvider, "DefaultDashboardProvider should be loaded via SPI");
	}
}
