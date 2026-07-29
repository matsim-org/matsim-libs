package org.matsim.contrib.emissions;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.emissions.utils.HbefaUtils;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

	private VehicleType createVehicleType(String technology, String emissionConcept) {
		VehicleType vehicleType = VehicleUtils.createVehicleType(Id.create("type", VehicleType.class));
		VehicleUtils.setHbefaTechnology(vehicleType.getEngineInformation(), technology);
		VehicleUtils.setHbefaEmissionsConcept(vehicleType.getEngineInformation(), emissionConcept);
		return vehicleType;
	}
}
