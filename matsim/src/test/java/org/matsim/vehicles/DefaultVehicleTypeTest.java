package org.matsim.vehicles;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

class DefaultVehicleTypeTest {
	@Test
	void testFirst() {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		Vehicles vehicles = scenario.getVehicles();

		VehicleType vehicleType = VehicleUtils.createDefaultVehicleType();
		vehicles.addVehicleType(vehicleType);

		VehicleType vehicleType2 = vehicles.getFactory().createVehicleType(Id.create("custom", VehicleType.class));
		vehicles.addVehicleType(vehicleType2);
	}

	@Test
	void testSecond() {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		Vehicles vehicles = scenario.getVehicles();

		VehicleType vehicleType = VehicleUtils.createDefaultVehicleType();
		vehicles.addVehicleType(vehicleType);

		// this caused a clash in IDs before getDefaultVehicleType was fixed
		VehicleType vehicleType2 = vehicles.getFactory().createVehicleType(Id.create("custom", VehicleType.class));
		vehicles.addVehicleType(vehicleType2);
	}
}