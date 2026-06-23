package org.matsim.contrib.shared_mobility.examples;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.shared_mobility.run.SharingConfigGroup;
import org.matsim.contrib.shared_mobility.run.SharingModule;
import org.matsim.contrib.shared_mobility.run.SharingServiceConfigGroup;
import org.matsim.contrib.shared_mobility.run.SharingServiceConfigGroup.ServiceScheme;
import org.matsim.contrib.shared_mobility.service.SharingService;
import org.matsim.contrib.shared_mobility.service.SharingUtils;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.RoutingConfigGroup.TeleportedModeParams;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.config.groups.ScoringConfigGroup.ModeParams;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.controler.Controler;

/**
 *
 * This is an example of a station-based bike-sharing service
 * siouxfalls-2014 can be used to run the simulation with the provided example
 * input file
 *
 */
public class RunTeleportationBikesharing {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.build();

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"));

		// We define bike to be routed based on Euclidean distance.
		config.routing().addTeleportedModeParams( new TeleportedModeParams("bike").setTeleportedModeSpeed(5.0 ).setBeelineDistanceFactor(1.3 ) );

		// Walk is deleted by adding bike here, we need to re-add it ...
		config.routing().addTeleportedModeParams( new TeleportedModeParams("walk").setTeleportedModeSpeed(2.0 ).setBeelineDistanceFactor(1.3 ) );

		// By default, "bike" will be simulated using teleportation.

		// We need to add the sharing config group
		SharingConfigGroup sharingConfig = new SharingConfigGroup();
		config.addModule(sharingConfig);

		// We need to define a service ...
		SharingServiceConfigGroup serviceConfig = new SharingServiceConfigGroup();
		sharingConfig.addService(serviceConfig);

		// ... with a service id. The respective mode will be "sharing:velib".
		serviceConfig.setIdFromString("velib");

		// ... with freefloating characteristics
		serviceConfig.setMaximumAccessEgressDistance(100000);
		serviceConfig.setServiceScheme(ServiceScheme.StationBased);
		serviceConfig.setServiceAreaShapeFile(null);

		// ... with a number of available vehicles and their initial locations the following file is an example and it works with
		// the siouxfalls-2014 scenario
		serviceConfig.setServiceInputFile("shared_taxi_vehicles_stations.xml");

		// ... and, we need to define the underlying mode, here "bike".
		serviceConfig.setMode("bike");

		// Finally, we need to make sure that the service mode (sharing:velib) is
		// considered in mode choice.
		List<String> modes = new ArrayList<>(Arrays.asList(config.subtourModeChoice().getModes()));
		modes.add(SharingUtils.getServiceMode(serviceConfig));
		config.subtourModeChoice().setModes(modes.toArray(new String[modes.size()]));

		// We need to add interaction activity types to scoring
		config.scoring().addActivityParams( new ActivityParams(SharingUtils.PICKUP_ACTIVITY).setScoringThisActivityAtAll(false ) );
		config.scoring().addActivityParams( new ActivityParams(SharingUtils.DROPOFF_ACTIVITY).setScoringThisActivityAtAll(false ) );
		config.scoring().addActivityParams( new ActivityParams(SharingUtils.BOOKING_ACTIVITY).setScoringThisActivityAtAll(false ) );

		// We need to score bike
		config.scoring().addModeParams( new ModeParams("bike") );

		// Write out all events (DEBUG)
		config.controller().setWriteEventsInterval(1);
		config.controller().setWritePlansInterval(1);
		config.controller().setLastIteration(10);

		// Set up controller (no specific settings needed for scenario)
		Controler controller = new Controler(config);

		// Does not really "override" anything
		controller.addOverridingModule(new SharingModule());

		// Enable QSim components
		controller.configureQSimComponents(SharingUtils.configureQSim(sharingConfig));

		controller.run();
	}
}
