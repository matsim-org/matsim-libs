package org.matsim.contrib.bicycle.run;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.bicycle.BicycleModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehiclesFactory;

public final class BicycleTestRunner {
	private static final String BICYCLE_MODE = "bicycle";
	private static final double BICYCLE_MAX_VELOCITY = 4.16666666;
	private static final double BICYCLE_PCU = 0.25;

	private BicycleTestRunner() {
	}

	public static void run(Config config) {
		Scenario scenario = ScenarioUtils.loadScenario(config);

		scenario.getConfig().qsim()
			.setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);

		VehiclesFactory vf = VehicleUtils.getFactory();

		scenario.getVehicles().addVehicleType(
			vf.createVehicleType(Id.createVehicleTypeId(TransportMode.car))
				.setNetworkMode(TransportMode.car)
		);

		scenario.getVehicles().addVehicleType(
			vf.createVehicleType(Id.createVehicleTypeId(BICYCLE_MODE))
				.setNetworkMode(BICYCLE_MODE)
				.setMaximumVelocity(BICYCLE_MAX_VELOCITY)
				.setPcuEquivalents(BICYCLE_PCU)
		);

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new BicycleModule());
		controler.run();
	}
}
