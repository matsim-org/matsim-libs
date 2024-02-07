package org.matsim.contrib.shared_mobility;

import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.contrib.shared_mobility.run.SharingConfigGroup;
import org.matsim.contrib.shared_mobility.run.SharingModule;
import org.matsim.contrib.shared_mobility.run.SharingServiceConfigGroup;
import org.matsim.contrib.shared_mobility.run.SharingServiceConfigGroup.ServiceScheme;
import org.matsim.contrib.shared_mobility.service.SharingUtils;
import org.matsim.contrib.shared_mobility.service.events.SharingDropoffEventHandler;
import org.matsim.contrib.shared_mobility.service.events.SharingFailedDropoffEventHandler;
import org.matsim.contrib.shared_mobility.service.events.SharingFailedPickupEventHandler;
import org.matsim.contrib.shared_mobility.service.events.SharingPickupEventHandler;
import org.matsim.contrib.shared_mobility.service.events.SharingVehicleEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.config.groups.ScoringConfigGroup.ModeParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.examples.ExamplesUtils;

public class RunIT {

	@Test
	final void test() throws UncheckedIOException, ConfigurationException, URISyntaxException {
		URL scenarioUrl = ExamplesUtils.getTestScenarioURL("siouxfalls-2014");


		Config config = ConfigUtils.loadConfig(ConfigGroup.getInputFileURL(scenarioUrl, "config_default.xml"));
		config.controller().setLastIteration(2);

		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		// We need to add the sharing config group
		SharingConfigGroup sharingConfig = new SharingConfigGroup();
		config.addModule(sharingConfig);

		// We need to define a service ...
		SharingServiceConfigGroup serviceConfig = new SharingServiceConfigGroup();
		sharingConfig.addService(serviceConfig);

		// ... with a service id. The respective mode will be "sharing:velib".
		serviceConfig.setId("mobility");

		// ... with freefloating characteristics
		serviceConfig.setMaximumAccessEgressDistance(100000);
		serviceConfig.setServiceScheme(ServiceScheme.StationBased);
		serviceConfig.setServiceAreaShapeFile(null);

		// ... with a number of available vehicles and their initial locations
		URL vehiclesUrl_mobility = RunIT.class.getResource("shared_vehicles_mobility.xml");
		serviceConfig.setServiceInputFile(vehiclesUrl_mobility.toURI().getPath());

		// ... and, we need to define the underlying mode, here "car".
		serviceConfig.setMode("car");

		// Finally, we need to make sure that the service mode (sharing:velib) is
		// considered in mode choice.
		List<String> modes = new ArrayList<>(Arrays.asList(config.subtourModeChoice().getModes()));
		modes.add(SharingUtils.getServiceMode(serviceConfig));
		config.subtourModeChoice().setModes(modes.toArray(new String[modes.size()]));

		SharingServiceConfigGroup serviceConfigBike = new SharingServiceConfigGroup();
		sharingConfig.addService(serviceConfigBike);

		// ... with a service id. The respective mode will be "sharing:velib".
		serviceConfigBike.setId("velib");

		// ... with freefloating characteristics
		serviceConfigBike.setMaximumAccessEgressDistance(100000);
		serviceConfigBike.setServiceScheme(ServiceScheme.StationBased);
		serviceConfigBike.setServiceAreaShapeFile(null);

		// ... with a number of available vehicles and their initial locations
		URL vehiclesUrl_velib = RunIT.class.getResource("shared_vehicles_velib.xml");
		serviceConfigBike.setServiceInputFile(vehiclesUrl_velib.toURI().getPath());

		// ... and, we need to define the underlying mode, here "car".
		serviceConfigBike.setMode("bike");

		// Finally, we need to make sure that the service mode (sharing:velib) is
		// considered in mode choice.
		modes = new ArrayList<>(Arrays.asList(config.subtourModeChoice().getModes()));
		modes.add(SharingUtils.getServiceMode(serviceConfigBike));
		config.subtourModeChoice().setModes(modes.toArray(new String[modes.size()]));

		SharingServiceConfigGroup serviceConfigBikeFF = new SharingServiceConfigGroup();
		sharingConfig.addService(serviceConfigBikeFF);

		// ... with a service id. The respective mode will be "sharing:velib".
		serviceConfigBikeFF.setId("wheels");

		// ... with freefloating characteristics
		serviceConfigBikeFF.setMaximumAccessEgressDistance(100000);
		serviceConfigBikeFF.setServiceScheme(ServiceScheme.Freefloating);
		serviceConfigBikeFF.setServiceAreaShapeFile(null);

		// ... with a number of available vehicles and their initial locations
		URL vehiclesUrl_wheels = RunIT.class.getResource("shared_vehicles_wheels.xml");
		serviceConfigBikeFF.setServiceInputFile(vehiclesUrl_wheels.toURI().getPath());

		// ... and, we need to define the underlying mode, here "car".
		serviceConfigBikeFF.setMode("bike");

		// Finally, we need to make sure that the service mode (sharing:velib) is
		// considered in mode choice.
		modes = new ArrayList<>(Arrays.asList(config.subtourModeChoice().getModes()));
		modes.add(SharingUtils.getServiceMode(serviceConfigBikeFF));
		config.subtourModeChoice().setModes(modes.toArray(new String[modes.size()]));

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

		// We need to score car
		ModeParams carScoringParams = new ModeParams("car");
		config.scoring().addModeParams(carScoringParams);

		// We need to score bike
		ModeParams bikeScoringParams = new ModeParams("bike");
		config.scoring().addModeParams(bikeScoringParams);

		// Set up controller (no specific settings needed for scenario)
		Controler controller = new Controler(config);

		// Does not really "override" anything
		controller.addOverridingModule(new SharingModule());

		// Enable QSim components
		controller.configureQSimComponents(SharingUtils.configureQSim(sharingConfig));

		controller.run();

		OutputData data = countLegs(controller.getControlerIO().getOutputPath() + "/output_events.xml.gz");

		Assertions.assertEquals(82629, (long) data.counts.get("car"));
		Assertions.assertEquals(29739, (long) data.counts.get("walk"));
		Assertions.assertEquals(31, (long) data.counts.get("bike"));
		Assertions.assertEquals(19029, (long) data.counts.get("pt"));

		Assertions.assertEquals(21, (long) data.pickupCounts.get("wheels"));
		Assertions.assertEquals(2, (long) data.pickupCounts.get("mobility"));
		Assertions.assertEquals(10, (long) data.pickupCounts.get("velib"));

		Assertions.assertEquals(21, (long) data.dropoffCounts.get("wheels"));
		Assertions.assertEquals(0, (long) data.dropoffCounts.getOrDefault("mobility", 0L));
		Assertions.assertEquals(10, (long) data.dropoffCounts.get("velib"));

		Assertions.assertEquals(0, (long) data.failedPickupCounts.getOrDefault("wheels",0L));
		Assertions.assertEquals(0, (long) data.failedPickupCounts.getOrDefault("mobility",0L));
		Assertions.assertEquals(0, (long) data.failedPickupCounts.getOrDefault("velib",0L));

		Assertions.assertEquals(0, (long) data.failedDropoffCounts.getOrDefault("wheels", 0L));
		Assertions.assertEquals(0, (long) data.failedDropoffCounts.getOrDefault("mobility", 0L));
		Assertions.assertEquals(0, (long) data.failedDropoffCounts.getOrDefault("velib", 0L));

		Assertions.assertEquals(2, (long) data.vehicleCounts.get("wheels"));
		Assertions.assertEquals(2, (long) data.vehicleCounts.get("mobility"));
		Assertions.assertEquals(2, (long) data.vehicleCounts.get("velib"));
	}

	static class OutputData {
		private final Map<String, Long> counts = new HashMap<>();
		private final Map<String, Long> pickupCounts = new HashMap<>();
		private final Map<String, Long> dropoffCounts = new HashMap<>();
		private final Map<String, Long> failedPickupCounts = new HashMap<>();
		private final Map<String, Long> failedDropoffCounts = new HashMap<>();
		private final Map<String, Long> vehicleCounts = new HashMap<>();
	}

	static OutputData countLegs(String eventsPath) {
		EventsManager manager = EventsUtils.createEventsManager();
		OutputData data = new OutputData();

		manager.addHandler((PersonDepartureEventHandler) event -> {
			data.counts.compute(event.getLegMode(), (k, v) -> v == null ? 1 : v + 1);
		});

		manager.addHandler((SharingPickupEventHandler) event -> {
			data.pickupCounts.compute(event.getServiceId().toString(), (k, v) -> v == null ? 1 : v + 1);
		});

		manager.addHandler((SharingDropoffEventHandler) event -> {
			data.dropoffCounts.compute(event.getServiceId().toString(), (k, v) -> v == null ? 1 : v + 1);
		});

		manager.addHandler((SharingFailedDropoffEventHandler) event -> {
			data.failedDropoffCounts.compute(event.getServiceId().toString(), (k, v) -> v == null ? 1 : v + 1);
		});

		manager.addHandler((SharingFailedPickupEventHandler) event -> {
			data.failedPickupCounts.compute(event.getServiceId().toString(), (k, v) -> v == null ? 1 : v + 1);
		});

		manager.addHandler((SharingVehicleEventHandler) event -> {
			data.vehicleCounts.compute(event.getServiceId().toString(), (k, v) -> v == null ? 1 : v + 1);
		});

		MatsimEventsReader reader = new MatsimEventsReader(manager);
		SharingUtils.addEventMappers(reader);
		reader.readFile(eventsPath);

		System.out.println("Leg counts:");
		for (Map.Entry<String, Long> entry : data.counts.entrySet()) {
			System.out.println("  " + entry.getKey() + " " + entry.getValue());
		}

		System.out.println("Pickup counts:");
		for (Map.Entry<String, Long> entry : data.pickupCounts.entrySet()) {
			System.out.println("  " + entry.getKey() + " " + entry.getValue());
		}

		System.out.println("Failed pickup counts:");
		for (Map.Entry<String, Long> entry : data.failedPickupCounts.entrySet()) {
			System.out.println("  " + entry.getKey() + " " + entry.getValue());
		}

		System.out.println("Dropoff counts:");
		for (Map.Entry<String, Long> entry : data.dropoffCounts.entrySet()) {
			System.out.println("  " + entry.getKey() + " " + entry.getValue());
		}

		System.out.println("Failed dropoff counts:");
		for (Map.Entry<String, Long> entry : data.failedDropoffCounts.entrySet()) {
			System.out.println("  " + entry.getKey() + " " + entry.getValue());
		}

		System.out.println("Vehicle counts:");
		for (Map.Entry<String, Long> entry : data.vehicleCounts.entrySet()) {
			System.out.println("  " + entry.getKey() + " " + entry.getValue());
		}

		return data;
	}

}
