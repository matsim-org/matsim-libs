package org.matsim.contrib.emissions;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.emissions.utils.HbefaUtils;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HbefaUtilsTest {

	@Test
	void switchesSwappedTechnologyAndEmissionConcept() {
		VehicleType vehicleType = createVehicleType("average", "diesel");

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getVehicles().addVehicleType(vehicleType);
		HbefaUtils.checkAndCorrectHbefaTechnologyAndEmissionConcept(scenario);

		assertEquals("diesel", VehicleUtils.getHbefaTechnology(vehicleType.getEngineInformation()));
		assertEquals("average", VehicleUtils.getHbefaEmissionsConcept(vehicleType.getEngineInformation()));
	}

	@Test
	void leavesCorrectlyConfiguredVehicleTypeUnchanged() {
		VehicleType vehicleType = createVehicleType("diesel", "average");

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getVehicles().addVehicleType(vehicleType);
		HbefaUtils.checkAndCorrectHbefaTechnologyAndEmissionConcept(scenario.getVehicles());

		assertEquals("diesel", VehicleUtils.getHbefaTechnology(vehicleType.getEngineInformation()));
		assertEquals("average", VehicleUtils.getHbefaEmissionsConcept(vehicleType.getEngineInformation()));
	}

	@Test
	void leavesAverageVehicleTypeUnchanged() {
		VehicleType vehicleType = createVehicleType("average", "average");

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getVehicles().addVehicleType(vehicleType);
		HbefaUtils.checkAndCorrectHbefaTechnologyAndEmissionConcept(scenario);

		assertEquals("average", VehicleUtils.getHbefaTechnology(vehicleType.getEngineInformation()));
		assertEquals("average", VehicleUtils.getHbefaEmissionsConcept(vehicleType.getEngineInformation()));
	}

	@Test
	void rejectsVehicleTypeWithDetailedTechnologyAndEmissionConcept() {
		VehicleType vehicleType = createVehicleType("diesel", "PC-D-Euro-3");

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getVehicles().addVehicleType(vehicleType);

		assertThrows(IllegalArgumentException.class, () -> HbefaUtils.checkAndCorrectHbefaTechnologyAndEmissionConcept(scenario));
		assertThrows(IllegalArgumentException.class, () -> HbefaUtils.checkAndCorrectHbefaTechnologyAndEmissionConcept(scenario.getVehicles()));
	}

	@Test
	void ignoresVehicleTypeWithMissingTechnologyOrEmissionConcept() {
		VehicleType missingTechnology = createVehicleType(null, "diesel");
		VehicleType missingEmissionConcept = createVehicleType("average", null);

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getVehicles().addVehicleType(missingTechnology);
		scenario.getVehicles().addVehicleType(missingEmissionConcept);

		assertDoesNotThrow(() -> HbefaUtils.checkAndCorrectHbefaTechnologyAndEmissionConcept(scenario));
		assertNull(VehicleUtils.getHbefaTechnology(missingTechnology.getEngineInformation()));
		assertEquals("diesel", VehicleUtils.getHbefaEmissionsConcept(missingTechnology.getEngineInformation()));
		assertEquals("average", VehicleUtils.getHbefaTechnology(missingEmissionConcept.getEngineInformation()));
		assertNull(VehicleUtils.getHbefaEmissionsConcept(missingEmissionConcept.getEngineInformation()));
	}

	private VehicleType createVehicleType(String technology, String emissionConcept) {
		String id = String.valueOf(technology) + '_' + emissionConcept;
		VehicleType vehicleType = VehicleUtils.createVehicleType(Id.create(id, VehicleType.class));
		if (technology != null) {
			VehicleUtils.setHbefaTechnology(vehicleType.getEngineInformation(), technology);
		}
		if (emissionConcept != null) {
			VehicleUtils.setHbefaEmissionsConcept(vehicleType.getEngineInformation(), emissionConcept);
		}
		return vehicleType;
	}
}
