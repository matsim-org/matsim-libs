package ch.sbb.matsim.contrib.railsim.qsimengine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.vehicles.MatsimVehicleReader;
import org.matsim.vehicles.Vehicle;


class TrainManagerTest {

	private Scenario scenario;
	private TrainManager trainManager;

	@BeforeEach
	void setUp() {
		// Load real test data from simpleFormation folder
		scenario = loadTestScenario();
		trainManager = new TrainManager(scenario);
	}

	/**
	 * Loads the real test scenario from the simpleFormation test data.
	 */
	private Scenario loadTestScenario() {
		// Create a minimal config
		Config config = ConfigUtils.createConfig();

		// Create scenario
		Scenario testScenario = ScenarioUtils.createScenario(config);

		// Load transit schedule
		TransitScheduleReader scheduleReader = new TransitScheduleReader(testScenario);
		scheduleReader.readFile("test/input/ch/sbb/matsim/contrib/railsim/integration/simpleFormation/transitSchedule.xml");

		// Load vehicles
		MatsimVehicleReader vehicleReader = new MatsimVehicleReader(testScenario.getTransitVehicles());
		vehicleReader.readFile("test/input/ch/sbb/matsim/contrib/railsim/integration/simpleFormation/transitVehicles.xml");

		return testScenario;
	}

	@Test
	void testFormationInitialization() {
		// Test that formations are properly initialized from real data
		assertThat(trainManager.getFormationCount()).isEqualTo(2);

		// Test formation retrieval for formation_1
		List<String> formation1 = trainManager.getFormation(Id.createVehicleId("formation_1"));
		assertThat(formation1).hasSize(2)
			.contains("train_A_1", "train_B_1");

		// Test formation retrieval for formation_2
		List<String> formation2 = trainManager.getFormation(Id.createVehicleId("formation_2"));
		assertThat(formation2).hasSize(2)
			.contains("train_A_2", "train_B_2");

		// Test non-existent formation
		List<String> noFormation = trainManager.getFormation(Id.createVehicleId("non_existent"));
		assertThat(noFormation).isEmpty();
	}

	@Test
	void testHasFormation() {
		// Test vehicles with formations
		assertThat(trainManager.hasFormation(Id.createVehicleId("formation_1"))).isTrue();
		assertThat(trainManager.hasFormation(Id.createVehicleId("formation_2"))).isTrue();

		// Test vehicles without formations
		assertThat(trainManager.hasFormation(Id.createVehicleId("train_A_1"))).isFalse();
		assertThat(trainManager.hasFormation(Id.createVehicleId("train_B_1"))).isFalse();
		assertThat(trainManager.hasFormation(Id.createVehicleId("non_existent"))).isFalse();
	}

	@Test
	void testGetAllFormations() {
		Map<Id<Vehicle>, List<String>> allFormations = trainManager.getAllFormations();
		assertThat(allFormations).hasSize(2)
			.containsKeys(Id.createVehicleId("formation_1"), Id.createVehicleId("formation_2"));
	}

	/**
	 * Helper method to test if two vehicles are related using getRelatedVehicles.
	 */
	private boolean areVehiclesRelated(Id<Vehicle> vehicleId1, Id<Vehicle> vehicleId2) {
		List<Id<Vehicle>> relatedToVehicle1 = trainManager.getRelatedVehicles(vehicleId1);
		return relatedToVehicle1.contains(vehicleId2);
	}

	/**
	 * Helper method to get all vehicles that have related vehicles.
	 */
	private Map<Id<Vehicle>, List<Id<Vehicle>>> getAllRelatedVehicles() {
		Map<Id<Vehicle>, List<Id<Vehicle>>> allRelated = new HashMap<>();

		// Check all vehicles in the scenario
		for (Vehicle vehicle : scenario.getTransitVehicles().getVehicles().values()) {
			Id<Vehicle> vehicleId = vehicle.getId();
			List<Id<Vehicle>> related = trainManager.getRelatedVehicles(vehicleId);
			if (!related.isEmpty()) {
				allRelated.put(vehicleId, related);
			}
		}

		return allRelated;
	}

	@Test
	void testAreVehiclesRelated() {
		// Test that vehicles in the same formation are related
		assertThat(areVehiclesRelated(Id.createVehicleId("train_A_1"), Id.createVehicleId("train_B_1"))).isTrue();
		assertThat(areVehiclesRelated(Id.createVehicleId("train_A_1"), Id.createVehicleId("formation_1"))).isTrue();
		assertThat(areVehiclesRelated(Id.createVehicleId("train_B_1"), Id.createVehicleId("formation_1"))).isTrue();

		assertThat(areVehiclesRelated(Id.createVehicleId("train_A_2"), Id.createVehicleId("train_B_2"))).isTrue();

		// Test that vehicles from different formations are not related
		assertThat(areVehiclesRelated(Id.createVehicleId("train_A_1"), Id.createVehicleId("train_A_2"))).isFalse();
		assertThat(areVehiclesRelated(Id.createVehicleId("train_A_1"), Id.createVehicleId("train_B_2"))).isFalse();

		// Test non-existent vehicles
		assertThat(areVehiclesRelated(Id.createVehicleId("non_existent"), Id.createVehicleId("train_A_1"))).isFalse();
		assertThat(areVehiclesRelated(Id.createVehicleId("train_A_1"), Id.createVehicleId("non_existent"))).isFalse();
	}

	@Test
	void testGetAllRelatedVehicles() {
		Map<Id<Vehicle>, List<Id<Vehicle>>> allRelated = getAllRelatedVehicles();

		assertThat(allRelated).hasSize(4)
			.containsKeys(
				Id.createVehicleId("train_A_1"),
				Id.createVehicleId("train_B_1"),
				Id.createVehicleId("train_A_2"),
				Id.createVehicleId("train_B_2")
			);
	}
}
