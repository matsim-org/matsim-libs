package org.matsim.simwrapper.dashboard;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.freight.carriers.FreightCarriersConfigGroup;
import org.matsim.freight.carriers.controller.CarrierModule;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.SimWrapper;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.simwrapper.SimWrapperModule;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ExecutionException;



public class CarrierViewerDashboardTest {

	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();


	private void runCarrierScenario() {

		Config config;

		config = ConfigUtils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("freight-chessboard-9x9"), "config.xml"));
		config.plans().setInputFile(null); // remove passenger input
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setLastIteration(0);  // no iterations; for iterations see RunFreightWithIterationsExample.  kai, jan'23

		FreightCarriersConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfigGroup.setCarriersFile("singleCarrierFiveActivitiesWithoutRoutes.xml");
		freightConfigGroup.setCarriersVehicleTypesFile("vehicleTypes.xml");

		// load scenario (this is not loading the freight material):
		Scenario scenario = ScenarioUtils.loadScenario(config);

		//load carriers according to freight config
		CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);

		// Solving the VRP (generate carrier's tour plans)
		try {
			CarriersUtils.runJsprit(scenario);
		} catch (ExecutionException | InterruptedException e) {
			throw new RuntimeException(e);
		}

		SimWrapperConfigGroup group = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);
		group.setSampleSize(0.001);

		// ## MATSim configuration:  ##
		final Controler controler = new Controler(scenario);
		controler.addOverridingModule(new CarrierModule());
		controler.addOverridingModule(new SimWrapperModule());
		controler.run();
	}


	@Test
	void carrierViewer() {

		runCarrierScenario();

		Path out = Path.of(utils.getOutputDirectory());

		Assertions.assertThat(out)
			.isDirectoryContaining("glob:**output_carriers.xml.gz")
			.isDirectoryContaining("glob:**output_network.xml.gz");

	}
}
