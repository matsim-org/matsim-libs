package ch.sbb.matsim.contrib.railsim.integration;

import ch.sbb.matsim.contrib.railsim.RailsimModule;
import ch.sbb.matsim.contrib.railsim.qsimengine.RailsimQSimModule;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.vsp.scenario.SnzActivities;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;

import java.io.File;
import java.net.URL;
import java.util.Set;

public class RailsimIntegrationTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void scenario0() {

		File dir = new File(utils.getPackageInputDirectory(), "test0");

		Config config = ConfigUtils.loadConfig(new File(dir, "config.xml").toString());

		config.controler().setOutputDirectory(utils.getOutputDirectory());

		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);

		controler.addOverridingModule(new RailsimModule());
		controler.configureQSimComponents(components -> new RailsimQSimModule().configure(components));

		controler.run();

	}

	@Test
	public void scenario_genf() {

		File dir = new File(utils.getPackageInputDirectory(), "test_genf");

		Config config = ConfigUtils.loadConfig(new File(dir, "config.xml").toString());

		config.controler().setOutputDirectory(utils.getOutputDirectory());

		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);

		controler.addOverridingModule(new RailsimModule());
		controler.configureQSimComponents(components -> new RailsimQSimModule().configure(components));

		controler.run();

	}

	@Test
	public void scenario_kelheim() {

		URL base = ExamplesUtils.getTestScenarioURL("kelheim");

		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(base, "config.xml"));
		config.controler().setLastIteration(1);
		config.controler().setOutputDirectory(utils.getOutputDirectory());

		Scenario scenario = ScenarioUtils.loadScenario(config);

		// Convert all pt to rail
		for (Link link : scenario.getNetwork().getLinks().values()) {
			if (link.getAllowedModes().contains(TransportMode.car))
				link.setAllowedModes(Set.of(TransportMode.car, "ride", "freight"));

			if (link.getAllowedModes().contains(TransportMode.pt)) {
				link.setFreespeed(50);
				link.setAllowedModes(Set.of("rail"));
			}
		}

		// Maximum velocity must be configured
		for (VehicleType type : scenario.getTransitVehicles().getVehicleTypes().values()) {
			type.setMaximumVelocity(30);
			type.setLength(100);
		}

		SnzActivities.addScoringParams(config);

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new RailsimModule());
		controler.configureQSimComponents(components -> new RailsimQSimModule().configure(components));

		controler.run();
	}

}
