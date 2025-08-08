package org.matsim.contrib.ev.example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.EvModule;
import org.matsim.contrib.ev.discharging.AuxEnergyConsumption;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;
import org.matsim.contrib.ev.discharging.VehicleTypeSpecificDriveEnergyConsumptionFactory;
import org.matsim.contrib.ev.infrastructure.LTHConsumptionModelReader;
import org.matsim.contrib.ev.routing.TruckEvNetworkRoutingProvider;
import org.matsim.contrib.ev.util.EvLowSocPenaltyHandler;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

public class RunEvExampleTruckLTH {
	static final String DEFAULT_CONFIG_FILE = "test/input/org/matsim/contrib/ev/example/RunEvExample/config_trucks.xml";
	private static final Logger log = LogManager.getLogger(RunEvExampleTruckLTH.class);

	public static void main(String[] args) throws IOException {
		if (args.length > 0) {
			log.info("Starting simulation run with the following arguments:");
			log.info("args=" + Arrays.toString(args));
		} else {
			File localConfigFile = new File(DEFAULT_CONFIG_FILE);
			System.out.println("Working directory = " + System.getProperty("user.dir"));
			if (localConfigFile.exists()) {
				log.info("Starting simulation run with the local example config file");
				args = new String[]{DEFAULT_CONFIG_FILE};

			} else {
				// I couldn't check if this works as the config_trucks-file didn't exist already
				log.info("Starting simulation run with the example config file from GitHub repository");
				args = new String[]{"https://raw.githubusercontent.com/matsim-org/matsim/main/contribs/ev"
					+ DEFAULT_CONFIG_FILE};
			}
		}
		new RunEvExampleTruckLTH().run(args);
	}

	public void run( String[] args ) throws IOException {
		int[] iterations = new int[] {0, 1};

		for (int iteration : iterations) {


			Config config = ConfigUtils.loadConfig(args, new EvConfigGroup());
			config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
			config.controller().setOutputDirectory("./output/evTruckExample/ITER"+iteration+"/");

			Scenario scenario = ScenarioUtils.loadScenario(config);

			Controler controler = new Controler(scenario);

			// === 1. Add EV modules with custom consumption model
			VehicleTypeSpecificDriveEnergyConsumptionFactory driveFactory = new VehicleTypeSpecificDriveEnergyConsumptionFactory();
			var truckVehicleType = Id.create("HGV16", VehicleType.class);
			driveFactory.addEnergyConsumptionModelFactory(
				truckVehicleType,
				new LTHConsumptionModelReader().readURL(ConfigGroup.getInputFileURL(config.getContext(), "HGV16Map.csv"))
			);

			scenario.getNetwork().getLinks().values().forEach(link -> link.setAllowedModes(Set.of("car", "truck")));

			controler.addOverridingModule(new EvModule());
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bind(DriveEnergyConsumption.Factory.class).toInstance(driveFactory);
					bind(AuxEnergyConsumption.Factory.class).toInstance(ev -> (beginTime, duration, linkId) -> 0.0);

					// Use your dynamic charging-aware truck router
					addRoutingModuleBinding(TransportMode.car).toProvider(new TruckEvNetworkRoutingProvider(TransportMode.car));
					addRoutingModuleBinding(TransportMode.truck).toProvider(new TruckEvNetworkRoutingProvider(TransportMode.truck));

				}
			});
			EvLowSocPenaltyHandler penaltyHandler = new EvLowSocPenaltyHandler();
			// === 2. Add penalty handler to track missing charging
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bind(EvLowSocPenaltyHandler.class).asEagerSingleton();
					addEventHandlerBinding().toInstance(penaltyHandler);
					addControllerListenerBinding().toInstance(penaltyHandler);
				}
			});

			// === 3. Run simulation
			controler.run();

			// === 4. Output LEE information
			penaltyHandler.getDataFromLastIteration();

			// === 5. After simulation: place new chargers based on LEEs
			// To run this, you need at least a shapefile for aggregation of statistics
			// The method also demands a Corine Land Cover (CLC) data containing spatial information of land-use
			// If not wanted, the CLC part can be omitted but the code needs adjustments
			// This is left as an instruction of how to call the method to place charging stations based on LEEs

			//int[] plugs = new int[] {1, 2}; // nr of plugs at charging station. Will output a charging infrastructure for every integer nr of plugs.
			// try {
			// LowEnergyEventChargerPlacer.main(iteration + 1, plugs);
			// } catch (IOException e) {
			// throw new RuntimeException(e);
			// }

		}
	}
}

