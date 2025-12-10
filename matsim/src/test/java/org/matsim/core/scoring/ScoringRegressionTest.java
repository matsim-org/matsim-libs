package org.matsim.core.scoring;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ScoringRegressionTest {

	@RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testTeleportedTrip() {

		var config = loadEquilConfig();
		config.qsim().setMainModes(Collections.emptyList());

		var scenario = getScenario(config);

		var controler = new Controler(scenario);

		controler.addControllerListener(new ScoringListener() {
			@Override
			public double priority() {
				return Integer.MIN_VALUE;
			}

			@Override
			public void notifyScoring(ScoringEvent event) {
				assertEquals(-39239.973501685585, scenario.getPopulation().getPersons().values().iterator().next().getSelectedPlan().getScore(), 1e-19);
			}
		});
		controler.run();
	}

	@Test
	public void testNetworkTrip() {

		var config = loadEquilConfig();
		var scenario = getScenario(config);
		var controler = new Controler(scenario);
		controler.addControllerListener(new ScoringListener() {
			@Override
			public double priority() {
				return Integer.MIN_VALUE;
			}

			@Override
			public void notifyScoring(ScoringEvent event) {
				assertEquals(-39240.03597080516, scenario.getPopulation().getPersons().values().iterator().next().getSelectedPlan().getScore(), 1e-19);
			}
		});
		controler.run();
	}

	private static @NonNull Config loadEquilConfig() {
		var scenarioUrl = ExamplesUtils.getTestScenarioURL("equil");
		Path configPath = getConfigPath(scenarioUrl);

		var config = ConfigUtils.loadConfig(configPath.toString());
		config.controller().setLastIteration(0);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		var walkModeParam = new ScoringConfigGroup.ModeParams("walk")
			.setConstant(0)
			.setMarginalUtilityOfDistance(-1);
		config.scoring().addModeParams(walkModeParam);
		return config;
	}

	private static @NonNull Path getConfigPath(URL scenarioUrl) {
		Path configPath = null;
		try {
			configPath = Paths.get(scenarioUrl.toURI()).resolve("config_plans1.xml");
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
		return configPath;
	}

	private static @NonNull Scenario getScenario(Config config) {
		var scenario = ScenarioUtils.loadScenario(config);
		for (Person person : scenario.getPopulation().getPersons().values()) {
			PopulationUtils.resetRoutes(person.getSelectedPlan());
		}
		return scenario;
	}
}
