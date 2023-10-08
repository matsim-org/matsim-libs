package TransitCreator;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.*;

public class VehicleCreator {
	private final Scenario scenario;
	private final Vehicles vehicles;

	public VehicleCreator() {
		Config config = ConfigUtils.createConfig();
		this.scenario = ScenarioUtils.createScenario(config);
		this.vehicles = scenario.getVehicles();
	}

	public void createTrain(String scenarioPath, int numberOfVehicles) {
		// Define the vehicle type
		VehicleType trainType = vehicles.getFactory().createVehicleType(Id.create("pt", VehicleType.class));
		trainType.setMaximumVelocity(25.0); // 25 m/s or 90 km/h
		trainType.getCapacity().setSeats(333);
		trainType.getCapacity().setStandingRoom(667);
		trainType.setLength(36);



		vehicles.addVehicleType(trainType);

		// Create the vehicles based on the type
		for (int i = 0; i < numberOfVehicles; i++) {
			Id<Vehicle> id = Id.createVehicleId("tr_" + (i + 1));
			vehicles.addVehicle(vehicles.getFactory().createVehicle(id, trainType));
		}

		// Write the vehicles file
		new MatsimVehicleWriter(vehicles).writeFile(scenarioPath + "/transitVehicles.xml");
	}
}
