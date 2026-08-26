/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2025 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** *
 */

package org.matsim.contrib.drt.run.examples;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.passenger.DrtServiceTimeRequestValidator;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEventHandler;
import org.matsim.contrib.drt.prebooking.PrebookingParams;
import org.matsim.contrib.drt.prebooking.logic.ProbabilityBasedPrebookingLogic;
import org.matsim.contrib.drt.routing.DrtStopNetwork;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.DrtServiceConfigurationParams;
import org.matsim.contrib.drt.run.DrtServiceConfigurationsParams;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEvent;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEventHandler;
import org.matsim.contrib.dvrp.router.AttributeBasedStopFinder;
import org.matsim.contrib.dvrp.router.ClosestAccessEgressFacilityFinder;
import org.matsim.contrib.dvrp.router.DvrpRoutingModule.AccessEgressFacilityFinder;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTrees;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.examples.ExamplesUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

/**
 * Integration tests for the time-dependent {@code serviceConfiguration} of a DRT service, based on the Mielec example.
 *
 * @author nkuehnel / MOIA
 */
public class DrtServiceTimeIT {

	private static final double SERVICE_START = 6 * 3600;
	private static final double SERVICE_END = 10 * 3600;

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	/**
	 * A single service configuration covering the whole day must not change anything.
	 */
	@Test
	void testOneAllDayServiceConfigurationDoesNotChangeTheResults() {
		Id.resetCaches();
		Config config = stopBasedConfig();
		addServiceConfigurations(DrtConfigGroup.getSingleModeDrtConfig(config),
				serviceConfiguration("allDay", OptionalTime.undefined(), OptionalTime.undefined(), null));

		RunDrtExample.run(config, false);

		// the same values as RunDrtExampleIT.testRunDrtStopbasedExample
		var expectedStats = RunDrtExampleIT.Stats.newBuilder()
				.rejectionRate(0.05)
				.rejections(17)
				.waitAverage(260.24)
				.inVehicleTravelTimeMean(375.14)
				.totalTravelTimeMean(635.38)
				.build();

		RunDrtExampleIT.verifyDrtCustomerStatsCloseToExpectedStats(utils.getOutputDirectory(), expectedStats);
	}

	@Test
	void testServiceTimeSuppressesDrtOutsideTheWindow() {
		Id.resetCaches();
		Config config = stopBasedConfig();
		addServiceConfigurations(DrtConfigGroup.getSingleModeDrtConfig(config),
				serviceConfiguration("service", OptionalTime.defined(SERVICE_START), OptionalTime.defined(SERVICE_END),
						null));

		Controler controller = DrtControlerCreator.createControler(config, false);
		Tracker tracker = Tracker.install(controller);
		controller.run();

		assertRequestsFollowTheServiceTime(tracker);

		// outside the service time the routing produces walk legs of routing mode 'drt' (the fallback routing module)
		assertThat(tracker.drtFallbackDepartures.stream().filter(event -> event.getTime() >= SERVICE_END)).isNotEmpty();
	}

	@Test
	void testStopNetworksPerServiceConfiguration() {
		Id.resetCaches();
		Config config = stopBasedConfig();
		DrtConfigGroup drtConfig = DrtConfigGroup.getSingleModeDrtConfig(config);

		// split the Mielec stops into two stop networks and let each of them be served in one half of the day
		Map<String, Set<Id<Link>>> linksPerStopNetwork = writeStopsWithStopNetworks(config, drtConfig);
		addServiceConfigurations(drtConfig,
				serviceConfiguration("morning", OptionalTime.undefined(), OptionalTime.defined(12 * 3600), "even"),
				serviceConfiguration("afternoon", OptionalTime.defined(12 * 3600), OptionalTime.undefined(), "odd"));

		Controler controller = DrtControlerCreator.createControler(config, false);
		Tracker tracker = Tracker.install(controller);
		controller.run();

		assertThat(tracker.submitted).isNotEmpty();

		Set<Id<Request>> rejectedForServiceArea = tracker.rejected.stream()
				.filter(event -> hasCause(event, DrtServiceTimeRequestValidator.OUTSIDE_SERVICE_AREA_ACCESS_CAUSE)
						|| hasCause(event, DrtServiceTimeRequestValidator.OUTSIDE_SERVICE_AREA_EGRESS_CAUSE))
				.map(PassengerRequestRejectedEvent::getRequestId)
				.collect(Collectors.toSet());

		assertThat(tracker.submitted).allSatisfy(event -> {
			Set<Id<Link>> servedLinks = linksPerStopNetwork.get(
					event.getEarliestDepartureTime() < 12 * 3600 ? "even" : "odd");
			if (servedLinks.contains(event.getFromLinkId()) && servedLinks.contains(event.getToLinkId())) {
				return;
			}
			// the request was routed in one half of the day but the departure drifted into the other one, where its
			// stops are not served any more. The validator is the safety net for exactly this case.
			assertThat(rejectedForServiceArea).contains(event.getRequestId());
		});
		// both configurations are actually used
		assertThat(tracker.submitted.stream().filter(e -> e.getEarliestDepartureTime() < 12 * 3600)).isNotEmpty();
		assertThat(tracker.submitted.stream().filter(e -> e.getEarliestDepartureTime() >= 12 * 3600)).isNotEmpty();
	}

	@Test
	void testServiceTimeForDoor2Door() {
		Id.resetCaches();
		Config config = door2doorConfig();
		addServiceConfigurations(DrtConfigGroup.getSingleModeDrtConfig(config),
				serviceConfiguration("service", OptionalTime.defined(SERVICE_START), OptionalTime.defined(SERVICE_END),
						null));

		Controler controller = DrtControlerCreator.createControler(config, false);
		Tracker tracker = Tracker.install(controller);
		controller.run();

		assertRequestsFollowTheServiceTime(tracker);
		assertThat(tracker.drtFallbackDepartures.stream().filter(event -> event.getTime() >= SERVICE_END)).isNotEmpty();
	}

	/**
	 * The validator is the safety net for requests which reach the optimizer without having been routed under the
	 * service configuration (prebooking, within-day replanning, input plans carrying finished DRT routes). Here this is
	 * emulated by binding a time-unaware stop finder, so that the routing offers DRT around the clock.
	 */
	@Test
	void testValidatorRejectsRequestsWhichTheRoutingDidNotSuppress() {
		Id.resetCaches();
		Config config = stopBasedConfig();
		addServiceConfigurations(DrtConfigGroup.getSingleModeDrtConfig(config),
				serviceConfiguration("service", OptionalTime.defined(SERVICE_START), OptionalTime.defined(SERVICE_END),
						null));

		Controler controller = DrtControlerCreator.createControler(config, false);
		bindTimeUnawareStopFinder(controller);
		Tracker tracker = Tracker.install(controller);
		controller.run();

		assertThat(tracker.submitted.stream()
				.filter(event -> event.getEarliestDepartureTime() >= SERVICE_END)).isNotEmpty();
		assertThat(tracker.rejected.stream()
				.filter(event -> hasCause(event, DrtServiceTimeRequestValidator.OUTSIDE_SERVICE_TIME_CAUSE))).isNotEmpty();
	}

	/**
	 * Prebooked requests are validated at booking time, i.e. long before the desired departure. The end of the service
	 * time stays soft, so requests which start inside the window may be dropped off after it.
	 */
	@Test
	void testPrebookingIsValidatedAtBookingTime() {
		Id.resetCaches();
		Config config = stopBasedConfig();
		DrtConfigGroup drtConfig = DrtConfigGroup.getSingleModeDrtConfig(config);
		addServiceConfigurations(drtConfig,
				serviceConfiguration("service", OptionalTime.defined(SERVICE_START), OptionalTime.defined(SERVICE_END),
						null));
		PrebookingParams prebookingParams = new PrebookingParams();
		prebookingParams.setAbortRejectedPrebookings(false);
		drtConfig.addParameterSet(prebookingParams);

		Controler controller = DrtControlerCreator.createControler(config, false);
		// again a time-unaware stop finder, otherwise the routing would not even offer DRT outside the window
		bindTimeUnawareStopFinder(controller);
		ProbabilityBasedPrebookingLogic.install(controller, drtConfig, 0.5, 4.0 * 3600.0);
		Tracker tracker = Tracker.install(controller);
		controller.run();

		Map<Id<Request>, Double> desiredDepartureTimes = desiredDepartureTimes(tracker);

		List<PassengerRequestRejectedEvent> prebookedRejections = tracker.rejected.stream()
				.filter(event -> hasCause(event, DrtServiceTimeRequestValidator.OUTSIDE_SERVICE_TIME_CAUSE))
				.filter(event -> event.getRequestId().toString().contains("prebooked"))
				.toList();

		assertThat(prebookedRejections).isNotEmpty();
		// the rejection happens when the request is booked, not when the passenger wants to depart
		assertThat(prebookedRejections).allSatisfy(event -> assertThat(event.getTime()).isLessThan(
				desiredDepartureTimes.get(event.getRequestId())));

		// the end of the service time is soft: rides which start inside the window may end after it
		assertThat(tracker.droppedOff.stream().filter(event -> event.getTime() > SERVICE_END)).isNotEmpty();
	}

	/**
	 * The routing decides on the planned departure time, so DRT is never offered before the service starts. The actual
	 * departure may still drift past the end of the window (a delayed preceding leg); such requests are then rejected
	 * by the validator, because the service is closed at the time the passenger really wants to depart.
	 */
	private static void assertRequestsFollowTheServiceTime(Tracker tracker) {
		assertThat(tracker.submitted).isNotEmpty();
		assertThat(tracker.submitted).allSatisfy(
				event -> assertThat(event.getEarliestDepartureTime()).isGreaterThanOrEqualTo(SERVICE_START));
		assertThat(tracker.submitted.stream()
				.filter(event -> event.getEarliestDepartureTime() < SERVICE_END)).isNotEmpty();

		Map<Id<Request>, Double> desiredDepartureTimes = desiredDepartureTimes(tracker);
		assertThat(tracker.rejected.stream()
				.filter(event -> hasCause(event, DrtServiceTimeRequestValidator.OUTSIDE_SERVICE_TIME_CAUSE))).allSatisfy(
				event -> assertThat(desiredDepartureTimes.get(event.getRequestId())).isGreaterThanOrEqualTo(SERVICE_END));
	}

	/**
	 * {@link org.matsim.contrib.dvrp.passenger.InternalPassengerHandling} joins all causes of one rejection into a
	 * single string.
	 */
	private static boolean hasCause(PassengerRequestRejectedEvent event, String cause) {
		return Arrays.asList(event.getCause().split(", ")).contains(cause);
	}

	private static Map<Id<Request>, Double> desiredDepartureTimes(Tracker tracker) {
		return tracker.submitted.stream()
				.collect(Collectors.toMap(DrtRequestSubmittedEvent::getRequestId,
						DrtRequestSubmittedEvent::getEarliestDepartureTime, (a, b) -> a));
	}

	private Config stopBasedConfig() {
		return config("mielec_stop_based_drt_config.xml");
	}

	private Config door2doorConfig() {
		return config("mielec_drt_config.xml");
	}

	private Config config(String configFile) {
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), configFile);
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		return config;
	}

	private static void addServiceConfigurations(DrtConfigGroup drtConfig,
			DrtServiceConfigurationParams... serviceConfigurations) {
		DrtServiceConfigurationsParams params = new DrtServiceConfigurationsParams();
		for (DrtServiceConfigurationParams serviceConfiguration : serviceConfigurations) {
			params.addParameterSet(serviceConfiguration);
		}
		drtConfig.addParameterSet(params);
	}

	private static DrtServiceConfigurationParams serviceConfiguration(String name, OptionalTime startTime,
			OptionalTime endTime, String stopNetwork) {
		DrtServiceConfigurationParams serviceConfiguration = new DrtServiceConfigurationParams(name);
		serviceConfiguration.setStartTime(startTime);
		serviceConfiguration.setEndTime(endTime);
		serviceConfiguration.setStopNetwork(stopNetwork);
		return serviceConfiguration;
	}

	/**
	 * Assigns every second Mielec stop to the stop network "even" and the others to "odd", writes the resulting stops
	 * file next to the output directory and points the config to it.
	 *
	 * @return the links of the stops per stop network
	 */
	private Map<String, Set<Id<Link>>> writeStopsWithStopNetworks(Config config, DrtConfigGroup drtConfig) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new TransitScheduleReader(scenario).readURL(
				IOUtils.extendUrl(config.getContext(), drtConfig.getTransitStopFile()));

		Map<String, Set<Id<Link>>> linksPerStopNetwork = new HashMap<>();
		linksPerStopNetwork.put("even", new HashSet<>());
		linksPerStopNetwork.put("odd", new HashSet<>());

		// several Mielec stops share a link, and the test tells the two stop networks apart by their links, so the
		// split happens per link and not per stop
		Map<Id<Link>, String> stopNetworkPerLink = new HashMap<>();
		for (TransitStopFacility stop : scenario.getTransitSchedule().getFacilities().values()) {
			String stopNetwork = stopNetworkPerLink.get(stop.getLinkId());
			if (stopNetwork == null) {
				stopNetwork = stopNetworkPerLink.size() % 2 == 0 ? "even" : "odd";
				stopNetworkPerLink.put(stop.getLinkId(), stopNetwork);
			}
			stop.getAttributes()
					.putAttribute(AttributeBasedStopFinder.FACILITY_STOP_NETWORKS_ATTRIBUTE, stopNetwork);
			linksPerStopNetwork.get(stopNetwork).add(stop.getLinkId());
		}
		assertThat(linksPerStopNetwork.get("even")).doesNotContainAnyElementsOf(linksPerStopNetwork.get("odd"));

		// the output directory itself is deleted when the run starts, and the path must be absolute because the
		// config context points into the examples jar
		Path stopsFile = Paths.get(utils.getOutputDirectory())
				.toAbsolutePath()
				.getParent()
				.resolve("drtstops_with_stop_networks.xml");
		new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(stopsFile.toString());
		drtConfig.setTransitStopFile(stopsFile.toString());

		return linksPerStopNetwork;
	}

	/**
	 * Replaces the time-dependent stop finder by one which offers DRT at all times, in order to test the validator in
	 * isolation from the routing.
	 */
	private static void bindTimeUnawareStopFinder(Controler controller) {
		DrtConfigGroup drtConfig = DrtConfigGroup.getSingleModeDrtConfig(controller.getConfig());
		controller.addOverridingModule(new AbstractDvrpModeModule(drtConfig.getMode()) {
			@Override
			public void install() {
				bindModal(AccessEgressFacilityFinder.class).toProvider(modalProvider(getter -> {
					Network network = getter.get(Network.class);
					return new ClosestAccessEgressFacilityFinder(Double.MAX_VALUE, network,
							QuadTrees.createQuadTree(getter.getModal(DrtStopNetwork.class).getDrtStops().values()));
				})).asEagerSingleton();
			}
		});
	}

	private static class Tracker
			implements DrtRequestSubmittedEventHandler, PassengerRequestRejectedEventHandler,
			PassengerDroppedOffEventHandler, PersonDepartureEventHandler {

		private final List<DrtRequestSubmittedEvent> submitted = new ArrayList<>();
		private final List<PassengerRequestRejectedEvent> rejected = new ArrayList<>();
		private final List<PassengerDroppedOffEvent> droppedOff = new ArrayList<>();
		private final List<PersonDepartureEvent> drtFallbackDepartures = new ArrayList<>();

		@Override
		public void handleEvent(DrtRequestSubmittedEvent event) {
			submitted.add(event);
		}

		@Override
		public void handleEvent(PassengerRequestRejectedEvent event) {
			rejected.add(event);
		}

		@Override
		public void handleEvent(PassengerDroppedOffEvent event) {
			droppedOff.add(event);
		}

		@Override
		public void handleEvent(PersonDepartureEvent event) {
			if (TransportMode.drt.equals(event.getRoutingMode()) && !TransportMode.drt.equals(event.getLegMode())) {
				drtFallbackDepartures.add(event);
			}
		}

		private static Tracker install(Controler controller) {
			Tracker tracker = new Tracker();
			controller.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					addEventHandlerBinding().toInstance(tracker);
				}
			});
			return tracker;
		}
	}
}
