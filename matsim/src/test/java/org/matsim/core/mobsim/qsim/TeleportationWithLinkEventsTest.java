package org.matsim.core.mobsim.qsim;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;

import java.net.URL;
import java.util.List;

class TeleportationWithLinkEventsTest {

	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();


	@Test
	void testEventsEqualWithRoutedTeleporting() {

		URL kelheim = ExamplesUtils.getTestScenarioURL("kelheim");
		Config config = utils.loadConfig(IOUtils.extendUrl(kelheim, "config.xml"));

		config.controller().setLastIteration(1);
		config.controller().setWriteEventsInterval(2);
		config.controller().setOutputDirectory(utils.getOutputDirectory());

		// Enable bike as link teleported mode
		config.routing().setNetworkModes(List.of("car", "ride", "bike", "freight"));
		config.routing().setTeleportedRoutedModes(List.of("bike"));
		config.routing().removeTeleportedModeParams("bike");

		Scenario scenario = ScenarioUtils.loadScenario(config);
		prepareScenario(scenario);

		Controler controler = new Controler(scenario);
		// Empty scoring
		controler.setScoringFunctionFactory(person -> new SumScoringFunction());

		controler.run();

	}


	private void prepareScenario(Scenario scenario) {

		VehicleType bike = scenario.getVehicles().getFactory().createVehicleType(Id.create("bike", VehicleType.class));
		bike.setNetworkMode("bike");
		bike.setMaximumVelocity(12.0 / 3.6);

		scenario.getVehicles().addVehicleType(bike);
	}

}
