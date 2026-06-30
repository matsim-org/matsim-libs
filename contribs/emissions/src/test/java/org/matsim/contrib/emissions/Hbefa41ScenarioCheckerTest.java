package org.matsim.contrib.emissions;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.emissions.scenarioCheckers.Hbefa41ScenarioChecker;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import static org.junit.jupiter.api.Assertions.*;

class Hbefa41ScenarioCheckerTest {

	private final Hbefa41ScenarioChecker checker = new Hbefa41ScenarioChecker();

	@Test
	void validTechnologyShouldPass() {
		Scenario scenario = createScenario(
			VspExperimentalConfigGroup.VspDefaultsCheckingLevel.abort,
			"diesel"
		);

		assertDoesNotThrow(() -> checker.checkConsistencyBeforeRun(scenario));
	}

	@Test
	void averageTechnologyShouldPass() {
		Scenario scenario = createScenario(
			VspExperimentalConfigGroup.VspDefaultsCheckingLevel.abort,
			"average"
		);

		assertDoesNotThrow(() -> checker.checkConsistencyBeforeRun(scenario));
	}

	@Test
	void invalidTechnologyShouldThrowWhenAbortEnabled() {
		Scenario scenario = createScenario(
			VspExperimentalConfigGroup.VspDefaultsCheckingLevel.abort,
			"invalidTechnology"
		);

		RuntimeException exception = assertThrows(
			RuntimeException.class,
			() -> checker.checkConsistencyBeforeRun(scenario)
		);

		assertTrue(exception.getMessage().contains("vsp-abort"));
	}

	@Test
	void invalidTechnologyShouldNotThrowWhenWarnEnabled() {
		Scenario scenario = createScenario(
			VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn,
			"invalidTechnology"
		);

		assertDoesNotThrow(() -> checker.checkConsistencyBeforeRun(scenario));
	}

	@Test
	void mixtureOfValidAndInvalidVehicleTypesShouldThrow() {
		Scenario scenario = createScenario(
			VspExperimentalConfigGroup.VspDefaultsCheckingLevel.abort
		);

		addVehicleType(scenario, "diesel");
		addVehicleType(scenario, "electricity");
		addVehicleType(scenario, "invalidTechnology");

		assertThrows(
			RuntimeException.class,
			() -> checker.checkConsistencyBeforeRun(scenario)
		);
	}

	@Test
	void allVehicleTypesValidShouldPass() {
		Scenario scenario = createScenario(
			VspExperimentalConfigGroup.VspDefaultsCheckingLevel.abort
		);

		addVehicleType(scenario, "diesel");
		addVehicleType(scenario, "electricity");
		addVehicleType(scenario, "average");

		assertDoesNotThrow(() -> checker.checkConsistencyBeforeRun(scenario));
	}

	@Test
	void ignoreLevelShouldSkipChecks() {
		Scenario scenario = createScenario(
			VspExperimentalConfigGroup.VspDefaultsCheckingLevel.ignore,
			"invalidTechnology"
		);

		assertDoesNotThrow(() -> checker.checkConsistencyBeforeRun(scenario));
	}

	private Scenario createScenario(
		VspExperimentalConfigGroup.VspDefaultsCheckingLevel level,
		String technology
	) {
		Scenario scenario = createScenario(level);
		addVehicleType(scenario, technology);
		return scenario;
	}

	private Scenario createScenario(
		VspExperimentalConfigGroup.VspDefaultsCheckingLevel level
	) {
		Config config = ConfigUtils.createConfig();

		config.addModule(new EmissionsConfigGroup());
		config.vspExperimental().setVspDefaultsCheckingLevel(level);

		return ScenarioUtils.createScenario(config);
	}

	private void addVehicleType(Scenario scenario, String technology) {
		String id = "type_" + scenario.getVehicles().getVehicleTypes().size();

		VehicleType vehicleType =
			VehicleUtils.createVehicleType(Id.create(id, VehicleType.class));

		VehicleUtils.setHbefaTechnology(
			vehicleType.getEngineInformation(),
			technology
		);

		scenario.getVehicles().addVehicleType(vehicleType);
	}
}
