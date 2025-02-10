package org.matsim.contrib.shared_mobility.examples;

import org.matsim.contrib.shared_mobility.run.Activities;
import org.matsim.contrib.shared_mobility.run.SharingConfigGroup;
import org.matsim.contrib.shared_mobility.run.SharingModule;
import org.matsim.contrib.shared_mobility.run.SharingServiceConfigGroup;
import org.matsim.contrib.shared_mobility.run.SharingServiceConfigGroup.ServiceScheme;
import org.matsim.contrib.shared_mobility.service.SharingUtils;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.config.groups.ScoringConfigGroup.ModeParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.simwrapper.SimWrapperConfigGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * This is an example of a station-based roller-sharing service
 * input file
 *
 */
public class RunTeleportationRollersharing {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.build();

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"));
		config.addModule(new SimWrapperConfigGroup());
		Activities.addScoringParams(config, true);
		// We define bike to be routed based on Euclidean distance.
		//RoutingConfigGroup.TeleportedModeParams bikeRoutingParams = new RoutingConfigGroup.TeleportedModeParams("bike");
		//bikeRoutingParams.setTeleportedModeSpeed(5.0);
		//bikeRoutingParams.setBeelineDistanceFactor(1.3);
		//config.routing().addTeleportedModeParams(bikeRoutingParams);

		// Walk is deleted by adding bike here, we need to re-add it ...
		//RoutingConfigGroup.TeleportedModeParams walkRoutingParams = new RoutingConfigGroup.TeleportedModeParams("walk");
		//walkRoutingParams.setTeleportedModeSpeed(2.0);
		//walkRoutingParams.setBeelineDistanceFactor(1.3);
		//config.routing().addTeleportedModeParams(walkRoutingParams);

		// By default, "bike" will be simulated using teleportation.

		// We need to add the sharing config group
		SharingConfigGroup sharingConfig = new SharingConfigGroup();
		config.addModule(sharingConfig);

		// Define a service ...
		SharingServiceConfigGroup serviceConfig = new SharingServiceConfigGroup();
		sharingConfig.addService(serviceConfig);

		// ... with a service id.
		serviceConfig.setId("roller");

		// ... with StationBased characteristics
		serviceConfig.setMaximumAccessEgressDistance(100000);
		serviceConfig.setServiceScheme(ServiceScheme.StationBased);
		serviceConfig.setServiceAreaShapeFile(null);

		// ... with a number of available vehicles and their initial locations
		serviceConfig.setServiceInputFile("shared_roller_vehicles_stations.xml");

		// ... and, we need to define the underlying mode, here "roller".
		serviceConfig.setMode("roller");

		// Finally, we need to make sure that the service mode is
		// considered in mode choice.
		List<String> modes = new ArrayList<>(Arrays.asList(config.subtourModeChoice().getModes()));
		modes.add(SharingUtils.getServiceMode(serviceConfig));
		config.subtourModeChoice().setModes(modes.toArray(new String[modes.size()]));


         // Required for all calibration strategies
		for (String subpopulation : List.of("person", "freight", "goodsTraffic", "commercialPersonTraffic", "commercialPersonTraffic_service")) {
			config.replanning().addStrategySettings(
				new ReplanningConfigGroup.StrategySettings()
					.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta)
					.setWeight(1.0)
					.setSubpopulation(subpopulation)
			);

			config.replanning().addStrategySettings(
				new ReplanningConfigGroup.StrategySettings()
					.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute)
					.setWeight(0.15)
					.setSubpopulation(subpopulation)
			);
		}

		config.replanning().addStrategySettings(
			new ReplanningConfigGroup.StrategySettings()
				.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator)
				.setWeight(0.15)
				.setSubpopulation("person")
		);

		config.replanning().addStrategySettings(
			new ReplanningConfigGroup.StrategySettings()
				.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice)
				.setWeight(0.15)
				.setSubpopulation("person")
		);

		// We need to add interaction activity types to scoring
		ActivityParams pickupParams = new ActivityParams(SharingUtils.PICKUP_ACTIVITY);
		pickupParams.setScoringThisActivityAtAll(false);
		config.scoring().addActivityParams(pickupParams);

		ActivityParams dropoffParams = new ActivityParams(SharingUtils.DROPOFF_ACTIVITY);
		dropoffParams.setScoringThisActivityAtAll(false);
		config.scoring().addActivityParams(dropoffParams);

		ActivityParams bookingParams = new ActivityParams(SharingUtils.BOOKING_ACTIVITY);
		bookingParams.setScoringThisActivityAtAll(false);
		config.scoring().addActivityParams(bookingParams);

		// We need to score roller
		ModeParams rollerScoringParams = new ModeParams("roller");
		config.scoring().addModeParams(rollerScoringParams);

		// Write out all events (DEBUG)
		config.controller().setWriteEventsInterval(1);
		config.controller().setWritePlansInterval(1);
		config.controller().setLastIteration(5);

		// Set up controller (no specific settings needed for scenario)
		Controler controller = new Controler(config);

		// Does not really "override" anything
		controller.addOverridingModule(new SharingModule());

		// Enable QSim components
		controller.configureQSimComponents(SharingUtils.configureQSim(sharingConfig));

		controller.run();
	}
}
