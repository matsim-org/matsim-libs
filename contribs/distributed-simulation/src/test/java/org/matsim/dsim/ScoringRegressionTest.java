package org.matsim.dsim;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.net.URL;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ScoringRegressionTest {

	@RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void allTeleported() {

		var config = loadConfig("equil", utils.getOutputDirectory(), "config_plans1.xml");
		config.qsim().setMainModes(Collections.emptyList());
		config.dsim().setNetworkModes(Set.of());

		var scenario = loadScenarioAndResetRoutes(config);
		var controler = new Controler(scenario);

		controler.run();

		var person = getSinglePerson(scenario);
		assertEquals(-39239.973501685585, person.getSelectedPlan().getScore(), 0.1);
	}

	@Test
	public void mainModeOnNetwork() {

		var config = loadConfig("equil", utils.getOutputDirectory(), "config_plans1.xml");
		var scenario = loadScenarioAndResetRoutes(config);
		var controler = new Controler(scenario);
		controler.run();

		var person = getSinglePerson(scenario);
		assertEquals(-39240.03597080516, person.getSelectedPlan().getScore(), 0.1);
	}

	@Test
	public void ptTrip() {

		var config = loadConfig("pt-simple-lineswitch", utils.getOutputDirectory());
		config.controller().setMobsim("qsim");
		var scenario = ScenarioUtils.loadScenario(config);
		var controler = new Controler(scenario);
		controler.run();

		var person = getSinglePerson(scenario);
		assertEquals(-57.284102507736776, person.getSelectedPlan().getScore(), 0.1);

	}

	/**
	 * Disable test, as results were wrong before implemented the feature we were checking the regression for. QSim and Dsim still produce different
	 * results as travel times are slightly different between both implementations.
	 */
	@Disabled
	@Test
	public void kelheim() {
		var scenarioUrl = ExamplesUtils.getTestScenarioURL("kelheim");
		var configPath = getConfigPath(scenarioUrl, "config.xml");

		var config2 = ConfigUtils.loadConfig(configPath);
		config2.controller().setOutputDirectory(utils.getOutputDirectory());
		config2.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		config2.controller().setLastIteration(0);
		config2.controller().setWritePlansInterval(1);
		config2.controller().setMobsim("qsim");
		config2.qsim().setStartTime(0);
		config2.qsim().setEndTime(24 * 3600);
		Activities.addScoringParams(config2);

		var scenario2 = ScenarioUtils.loadScenario(config2);
		// Need to prepare network for freight
		var carandfreight2 = Set.of(TransportMode.car, "freight", TransportMode.ride);
		scenario2.getNetwork().getLinks().values().parallelStream()
			.filter(l -> l.getAllowedModes().contains(TransportMode.car))
			.forEach(l -> l.setAllowedModes(carandfreight2));

		var controler2 = new Controler(scenario2);
		controler2.run();

//		var comparisonPopulation = scenario2.getPopulation();//PopulationUtils.readPopulation(utils.getInputDirectory() + "expected_experienced_plans.xml.gz");
//		for (var person : scenario.getPopulation().getPersons().values()) {
//			var ep = comparisonPopulation.getPersons().get(person.getId());
//			assertEquals(ep.getSelectedPlan().getScore(), person.getSelectedPlan().getScore(), 10, "Person " + person.getId() + " has different score");
//		}
	}

	private static @NonNull Config loadConfig(String scenarioName, String outputDir, String... configFile) {
		var scenarioUrl = ExamplesUtils.getTestScenarioURL(scenarioName);
		var configPath = getConfigPath(scenarioUrl, configFile);

		var config = ConfigUtils.loadConfig(configPath);
		config.controller().setLastIteration(2);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(outputDir);
		config.controller().setMobsim("dsim");
		config.controller().getSnapshotFormat().clear();

		config.dsim().setThreads(2);

		var walkModeParam = new ScoringConfigGroup.ModeParams("walk")
			.setConstant(0)
			.setMarginalUtilityOfDistance(-1);
		config.scoring().addModeParams(walkModeParam);
		return config;
	}

	private static @NonNull String getConfigPath(URL scenarioUrl, String... configFile) {

		assert configFile.length < 2 : "Only one config file is supported";

		var fileName = configFile.length == 0 ? "config.xml" : configFile[0];
		return scenarioUrl.toString() + fileName;
	}

	private static @NonNull Scenario loadScenarioAndResetRoutes(Config config) {
		var scenario = ScenarioUtils.loadScenario(config);
		for (Person person : scenario.getPopulation().getPersons().values()) {
			PopulationUtils.resetRoutes(person.getSelectedPlan());
		}
		return scenario;
	}

	private static @NonNull Person getSinglePerson(Scenario scenario) {
		var size = scenario.getPopulation().getPersons().size();
		assertEquals(1, size, "Expected scenario with one Person, but has: " + size);
		return scenario.getPopulation().getPersons().values().iterator().next();
	}
}
