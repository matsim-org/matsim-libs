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
import static org.matsim.contrib.drt.run.DrtServiceRegimesFixtures.serviceRegime;

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
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.passenger.DrtServiceRegimeRequestValidator;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEventHandler;
import org.matsim.contrib.drt.prebooking.PrebookingParams;
import org.matsim.contrib.drt.prebooking.logic.ProbabilityBasedPrebookingLogic;
import org.matsim.contrib.drt.routing.DrtStopNetwork;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.DrtServiceRegimeParams;
import org.matsim.contrib.drt.run.DrtServiceRegimesFixtures;
import org.matsim.contrib.drt.run.DrtServiceRegimesParams;
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
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTrees;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.GeoFileWriter;
import org.matsim.core.utils.gis.PolygonFeatureFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.examples.ExamplesUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

/**
 * Integration tests for the time-dependent {@code serviceRegime} of a DRT service, based on the Mielec example.
 *
 * @author nkuehnel / MOIA
 */
public class DrtServiceRegimesIT {

	private static final double SERVICE_START = 6 * 3600;
	private static final double SERVICE_END = 10 * 3600;

	/** the column names of shapefiles are limited to 10 characters, so the default 'stopNetworks' does not fit */
	private static final String SERVICE_AREA_ATTRIBUTE = "networks";

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	/**
	 * A single service regime covering the whole day must not change anything.
	 */
	@Test
	void testOneAllDayServiceRegimeDoesNotChangeTheResults() {
		Id.resetCaches();
		Config config = stopBasedConfig();
		addServiceRegimes(DrtConfigGroup.getSingleModeDrtConfig(config),
				serviceRegime("allDay", OptionalTime.undefined(), OptionalTime.undefined(), null));

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
		addServiceRegimes(DrtConfigGroup.getSingleModeDrtConfig(config),
				serviceRegime("service", OptionalTime.defined(SERVICE_START), OptionalTime.defined(SERVICE_END),
						null));

		Controler controller = DrtControlerCreator.createControler(config, false);
		Tracker tracker = Tracker.install(controller);
		controller.run();

		assertRequestsFollowTheServiceTime(tracker);

		// outside the service time the routing produces walk legs of routing mode 'drt' (the fallback routing module)
		assertThat(tracker.drtFallbackDepartures.stream().filter(event -> event.getTime() >= SERVICE_END)).isNotEmpty();
	}

	@Test
	void testStopNetworksPerServiceRegime() {
		Id.resetCaches();
		Config config = stopBasedConfig();
		DrtConfigGroup drtConfig = DrtConfigGroup.getSingleModeDrtConfig(config);

		// split the Mielec stops into two stop networks and let each of them be served in one half of the day
		Map<String, Set<Id<Link>>> linksPerStopNetwork = writeStopsWithStopNetworks(config, drtConfig);
		addServiceRegimes(drtConfig,
				serviceRegime("morning", OptionalTime.undefined(), OptionalTime.defined(12 * 3600), "even"),
				serviceRegime("afternoon", OptionalTime.defined(12 * 3600), OptionalTime.undefined(), "odd"));

		Controler controller = DrtControlerCreator.createControler(config, false);
		Tracker tracker = Tracker.install(controller);
		controller.run();

		assertThat(tracker.submitted).isNotEmpty();

		Set<Id<Request>> rejectedForServiceArea = tracker.rejected.stream()
				.filter(event -> hasCause(event, DrtServiceRegimeRequestValidator.OUTSIDE_SERVICE_AREA_ACCESS_CAUSE)
						|| hasCause(event, DrtServiceRegimeRequestValidator.OUTSIDE_SERVICE_AREA_EGRESS_CAUSE))
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
		// both regimes are actually used
		assertThat(tracker.submitted.stream().filter(e -> e.getEarliestDepartureTime() < 12 * 3600)).isNotEmpty();
		assertThat(tracker.submitted.stream().filter(e -> e.getEarliestDepartureTime() >= 12 * 3600)).isNotEmpty();
	}

	/**
	 * A {@code stopbased} service can have service areas as well, but there the polygons only tag the transit stops,
	 * they do not restrict the inventory. A service regime without a {@code stopNetwork} therefore still serves all
	 * stops, including those outside all polygons.
	 */
	@Test
	void testServiceAreasDoNotRestrictAStopbasedRegimeWithoutStopNetwork() {
		Id.resetCaches();
		Config config = stopBasedConfig();
		DrtConfigGroup drtConfig = DrtConfigGroup.getSingleModeDrtConfig(config);
		DrtServiceRegimesParams params = addServiceRegimes(drtConfig,
				serviceRegime("allDay", OptionalTime.undefined(), OptionalTime.undefined(), null));
		writeServiceAreas(readNetwork(config), drtConfig, params);

		RunDrtExample.run(config, false);

		// the same values as testOneAllDayServiceRegimeDoesNotChangeTheResults, i.e. all stops are still served
		var expectedStats = RunDrtExampleIT.Stats.newBuilder()
				.rejectionRate(0.05)
				.rejections(17)
				.waitAverage(260.24)
				.inVehicleTravelTimeMean(375.14)
				.totalTravelTimeMean(635.38)
				.build();
		RunDrtExampleIT.verifyDrtCustomerStatsCloseToExpectedStats(utils.getOutputDirectory(), expectedStats);

		// the stops are tagged nonetheless, so another regime could restrict itself to one of the areas
		List<TransitStopFacility> dumpedStops = readDumpedStops(drtConfig);
		assertThat(dumpedStops).isNotEmpty()
				.allSatisfy(stop -> assertThat(AttributeBasedStopFinder.parseStopNetworks(stop)).contains("morning"));
		assertThat(dumpedStops.stream()
				.filter(stop -> AttributeBasedStopFinder.parseStopNetworks(stop).contains("afternoon"))).isNotEmpty();
	}

	/**
	 * The stops of a {@code serviceAreaBased} service can also be derived from the polygons of the service
	 * regimes, i.e. without a hand-attributed network or stops file: every link inside a polygon becomes a stop
	 * of the service regimes named by that polygon.
	 */
	@Test
	void testServiceAreasPerServiceRegime() {
		Id.resetCaches();
		Config config = config("mielec_serviceArea_based_drt_config.xml");
		DrtConfigGroup drtConfig = DrtConfigGroup.getSingleModeDrtConfig(config);

		// the whole town is served in the morning, only its western half in the afternoon
		Network network = readNetwork(config);
		DrtServiceRegimesParams params = addServiceRegimes(drtConfig,
				serviceRegime("morning", OptionalTime.undefined(), OptionalTime.defined(12 * 3600), "morning"),
				serviceRegime("afternoon", OptionalTime.defined(12 * 3600), OptionalTime.undefined(),
						"afternoon"));
		PreparedGeometry westernHalf = writeServiceAreas(network, drtConfig, params);

		Controler controller = DrtControlerCreator.createControler(config, false);
		Tracker tracker = Tracker.install(controller);
		controller.run();

		// the same containment test as DrtServiceAreas, so that the expectation cannot drift from the implementation
		Set<Id<Link>> westernLinks = network.getLinks()
				.values()
				.stream()
				.filter(link -> westernHalf.contains(MGC.coord2Point(link.getToNode().getCoord())))
				.map(Link::getId)
				.collect(Collectors.toSet());
		assertThat(westernLinks).isNotEmpty().hasSizeLessThan(network.getLinks().size());

		Set<Id<Request>> rejectedForServiceArea = tracker.rejected.stream()
				.filter(event -> hasCause(event, DrtServiceRegimeRequestValidator.OUTSIDE_SERVICE_AREA_ACCESS_CAUSE)
						|| hasCause(event, DrtServiceRegimeRequestValidator.OUTSIDE_SERVICE_AREA_EGRESS_CAUSE))
				.map(PassengerRequestRejectedEvent::getRequestId)
				.collect(Collectors.toSet());

		// in the afternoon only the western half is served, in the morning the whole network is
		assertThat(tracker.submitted.stream().filter(event -> event.getEarliestDepartureTime() >= 12 * 3600))
				.isNotEmpty()
				.allSatisfy(event -> {
					if (westernLinks.contains(event.getFromLinkId()) && westernLinks.contains(event.getToLinkId())) {
						return;
					}
					// the departure drifted from the morning into the afternoon, where these stops are not served any
					// more, so the validator rejects the request
					assertThat(rejectedForServiceArea).contains(event.getRequestId());
				});
		assertThat(tracker.submitted.stream()
				.filter(event -> event.getEarliestDepartureTime() < 12 * 3600)
				.filter(event -> !westernLinks.contains(event.getFromLinkId()))).isNotEmpty();

		// the derived stops and their stop networks are dumped, so the service areas are documented in the output
		Map<Id<Link>, Set<String>> dumpedStopNetworks = readDumpedStopNetworks(drtConfig);
		assertThat(dumpedStopNetworks.keySet()).containsAnyElementsOf(westernLinks);
		assertThat(dumpedStopNetworks.keySet().stream().filter(linkId -> !westernLinks.contains(linkId))).isNotEmpty();
		assertThat(dumpedStopNetworks).allSatisfy((linkId, stopNetworks) -> {
			if (westernLinks.contains(linkId)) {
				assertThat(stopNetworks).containsExactlyInAnyOrder("morning", "afternoon");
			} else {
				assertThat(stopNetworks).containsExactly("morning");
			}
		});
	}

	@Test
	void testServiceTimeForDoor2Door() {
		Id.resetCaches();
		Config config = door2doorConfig();
		addServiceRegimes(DrtConfigGroup.getSingleModeDrtConfig(config),
				serviceRegime("service", OptionalTime.defined(SERVICE_START), OptionalTime.defined(SERVICE_END),
						null));

		Controler controller = DrtControlerCreator.createControler(config, false);
		Tracker tracker = Tracker.install(controller);
		controller.run();

		assertRequestsFollowTheServiceTime(tracker);
		assertThat(tracker.drtFallbackDepartures.stream().filter(event -> event.getTime() >= SERVICE_END)).isNotEmpty();
	}

	/**
	 * The validator is the safety net for requests which reach the optimizer without having been routed under the
	 * service regime (prebooking, within-day replanning, input plans carrying finished DRT routes). Here this is
	 * emulated by binding a time-unaware stop finder, so that the routing offers DRT around the clock.
	 */
	@Test
	void testValidatorRejectsRequestsWhichTheRoutingDidNotSuppress() {
		Id.resetCaches();
		Config config = stopBasedConfig();
		addServiceRegimes(DrtConfigGroup.getSingleModeDrtConfig(config),
				serviceRegime("service", OptionalTime.defined(SERVICE_START), OptionalTime.defined(SERVICE_END),
						null));

		Controler controller = DrtControlerCreator.createControler(config, false);
		bindTimeUnawareStopFinder(controller);
		Tracker tracker = Tracker.install(controller);
		controller.run();

		assertThat(tracker.submitted.stream()
				.filter(event -> event.getEarliestDepartureTime() >= SERVICE_END)).isNotEmpty();
		assertThat(tracker.rejected.stream()
				.filter(event -> hasCause(event, DrtServiceRegimeRequestValidator.OUTSIDE_SERVICE_TIME_CAUSE))).isNotEmpty();
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
		addServiceRegimes(drtConfig,
				serviceRegime("service", OptionalTime.defined(SERVICE_START), OptionalTime.defined(SERVICE_END),
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
				.filter(event -> hasCause(event, DrtServiceRegimeRequestValidator.OUTSIDE_SERVICE_TIME_CAUSE))
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
				.filter(event -> hasCause(event, DrtServiceRegimeRequestValidator.OUTSIDE_SERVICE_TIME_CAUSE))).allSatisfy(
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

	private static DrtServiceRegimesParams addServiceRegimes(DrtConfigGroup drtConfig,
			DrtServiceRegimeParams... serviceRegimes) {
		DrtServiceRegimesParams params = DrtServiceRegimesFixtures.serviceRegimesParams(serviceRegimes);
		drtConfig.addParameterSet(params);
		return params;
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

	private static Network readNetwork(Config config) {
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readURL(
				IOUtils.extendUrl(config.getContext(), config.network().getInputFile()));
		return network;
	}

	/**
	 * Writes two service areas next to the output directory and points the config to them: one covering the whole
	 * network and belonging to the stop network "morning", one covering its western half and belonging to "afternoon".
	 * The served area is the union of both, and the stops of the western half belong to both stop networks.
	 *
	 * @return the geometry of the western half
	 */
	private PreparedGeometry writeServiceAreas(Network network, DrtConfigGroup drtConfig,
			DrtServiceRegimesParams params) {
		double[] boundingBox = NetworkUtils.getBoundingBox(network.getNodes().values());
		// the boundary of a polygon does not belong to it, hence the margin around the network
		double margin = 1000;
		double minX = boundingBox[0] - margin;
		double minY = boundingBox[1] - margin;
		double maxX = boundingBox[2] + margin;
		double maxY = boundingBox[3] + margin;
		double middleX = 0.5 * (boundingBox[0] + boundingBox[2]);

		// the Mielec network is metric, its config declares WGS84, which is why the CRS is given explicitly here
		PolygonFeatureFactory factory = new PolygonFeatureFactory.Builder().setName("serviceAreas")
				.setCrs(MGC.getCRS("EPSG:32633"))
				// column names of shapefiles are limited to 10 characters, hence the short name
				.addAttribute(SERVICE_AREA_ATTRIBUTE, String.class)
				.create();
		Coordinate[] westernHalf = rectangle(minX, minY, middleX, maxY);
		Path serviceAreaFile = Paths.get(utils.getOutputDirectory())
				.toAbsolutePath()
				.getParent()
				.resolve("service_areas.shp");
		GeoFileWriter.writeGeometries(List.of(
				factory.createPolygon(rectangle(minX, minY, maxX, maxY), Map.of(SERVICE_AREA_ATTRIBUTE, "morning"),
						"wholeTown"),
				factory.createPolygon(westernHalf, Map.of(SERVICE_AREA_ATTRIBUTE, "afternoon"), "westernHalf")),
				serviceAreaFile.toString());

		// the areas of the service regimes replace the static service area
		drtConfig.setDrtServiceAreaShapeFile(null);
		params.setServiceAreaFile(serviceAreaFile.toString());
		params.setServiceAreaAttribute(SERVICE_AREA_ATTRIBUTE);

		return new PreparedGeometryFactory().create(new GeometryFactory().createPolygon(closed(westernHalf)));
	}

	private static Coordinate[] rectangle(double minX, double minY, double maxX, double maxY) {
		return new Coordinate[] { new Coordinate(minX, minY), new Coordinate(maxX, minY), new Coordinate(maxX, maxY),
				new Coordinate(minX, maxY) };
	}

	private static Coordinate[] closed(Coordinate[] ring) {
		Coordinate[] closed = Arrays.copyOf(ring, ring.length + 1);
		closed[ring.length] = ring[0];
		return closed;
	}

	/**
	 * @return the stops written by {@link org.matsim.contrib.drt.util.DumpDrtStopsAtEnd}
	 */
	private List<TransitStopFacility> readDumpedStops(DrtConfigGroup drtConfig) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new TransitScheduleReader(scenario).readFile(
				Paths.get(utils.getOutputDirectory(), "output_drt_stops_" + drtConfig.getMode() + ".xml.gz").toString());
		return List.copyOf(scenario.getTransitSchedule().getFacilities().values());
	}

	/**
	 * @return the stop networks per link of the dumped stops. A {@code serviceAreaBased} service has exactly one stop
	 * per link, unlike a {@code stopbased} one, where several stops may share a link.
	 */
	private Map<Id<Link>, Set<String>> readDumpedStopNetworks(DrtConfigGroup drtConfig) {
		return readDumpedStops(drtConfig).stream()
				.collect(Collectors.toMap(TransitStopFacility::getLinkId,
						AttributeBasedStopFinder::parseStopNetworks));
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
