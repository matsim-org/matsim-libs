/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C)  by the members listed in the COPYING,            *
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
 * *********************************************************************** */

package org.matsim.freight.carriers.splitter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.freight.carriers.*;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class CarrierSplitterTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils() ;


	private static final String ORIGINAL_CARRIER_ATTRIBUTE = "originalCarrierAttribute";
	private static final String ORIGINAL_CARRIER_ATTRIBUTE_VALUE = "copied-to-split-carriers";
	private static final String ORIGINAL_VEHICLE_ATTRIBUTE = "originalVehicleAttribute";
	private static final String ORIGINAL_VEHICLE_ATTRIBUTE_VALUE = "copied-to-split-vehicles";
	private static final String DEPOT_LINK_ID = "j(0,1)R";
	private static final String SECOND_DEPOT_LINK_ID = "j(9,7)";
	private static final String[] CHESSBOARD_LINK_IDS = {
			"j(0,1)R", "j(1,3)", "j(2,5)R", "j(3,7)", "j(4,2)R", "i(5,6)", "j(6,1)R", "i(7,7)R", "j(8,4)R", "j(9,2)"
	};

	@Test
	void splitsShipmentCarrierWithRandomClustering() {
		assertShipmentCarrierSplit(CarrierSplitter.ClusteringStrategy.RANDOM);
	}

	@Test
	void splitsShipmentCarrierWithGreedyClustering() {
		assertShipmentCarrierSplit(CarrierSplitter.ClusteringStrategy.GREEDY);
	}

	@Test
	void splitsShipmentCarrierWithSingleLinkClustering() {
		assertShipmentCarrierSplit(CarrierSplitter.ClusteringStrategy.SINGLE_LINK);
	}

	@Test
	void splitsShipmentCarrierWithCentroidsClustering() {
		assertShipmentCarrierSplit(CarrierSplitter.ClusteringStrategy.CENTROIDS);
	}

	@Test
	void splitsServiceCarrierWithRandomClustering() {
		assertServiceCarrierSplit(CarrierSplitter.ClusteringStrategy.RANDOM);
	}

	@Test
	void splitsServiceCarrierWithGreedyClustering() {
		assertServiceCarrierSplit(CarrierSplitter.ClusteringStrategy.GREEDY);
	}

	@Test
	void splitsServiceCarrierWithSingleLinkClustering() {
		assertServiceCarrierSplit(CarrierSplitter.ClusteringStrategy.SINGLE_LINK);
	}

	@Test
	void splitsServiceCarrierWithCentroidsClustering() {
		assertServiceCarrierSplit(CarrierSplitter.ClusteringStrategy.CENTROIDS);
	}

	@Test
	void rejectsShipmentSplitWithoutExplicitShipmentLocation() {
		Scenario scenario = createShipmentScenario();

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> CarriersUtils.splitCarriers(scenario, CarrierSplitter.ClusteringStrategy.RANDOM, null, 2));

		assertEquals("shipmentClusterLocation must be specified explicitly when shipments are present.", exception.getMessage());
	}

	@Test
	void rejectsFiniteFleetCarriers() {
		Scenario scenario = createShipmentScenario();
		Carrier originalCarrier = CarriersUtils.getCarriers(scenario).getCarriers().values().iterator().next();
		originalCarrier.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.FINITE);

		UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
				() -> CarriersUtils.splitCarriers(
						scenario,
						CarrierSplitter.ClusteringStrategy.RANDOM,
						CarrierSplitter.ShipmentClusteringLocation.DELIVERY,
						2));

		assertEquals("Splitting carriers with finite fleets is not implemented or tested yet.", exception.getMessage());
	}

	@Test
	void rejectsGreedyClusteringWhenVehicleStartLinksDiffer() {
		Scenario scenario = createShipmentScenario();
		Carrier originalCarrier = CarriersUtils.getCarriers(scenario).getCarriers().values().iterator().next();
		originalCarrier.setCarrierCapabilities(createCarrierCapabilities(
				originalCarrier.getId().toString(),
			SECOND_DEPOT_LINK_ID));

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> CarriersUtils.splitCarriers(
						scenario,
						CarrierSplitter.ClusteringStrategy.GREEDY,
						CarrierSplitter.ShipmentClusteringLocation.DELIVERY,
						2));

		assertEquals("Greedy clustering requires all vehicles of carrier shipmentCarrier to have the same start link.",
				exception.getMessage());
	}

	@Test
	void serviceCarrierDoesNotRequireShipmentLocation() {
		Scenario scenario = createServiceScenario();

		CarriersUtils.splitCarriers(scenario, CarrierSplitter.ClusteringStrategy.RANDOM, null, 10);

		assertEquals(20, countServices(CarriersUtils.getCarriers(scenario)));
	}

	@Test
	void writesCarrierSplitPlotToOutputDirectory() {
		Scenario scenario = createShipmentScenario();
		scenario.getConfig().controller().setOutputDirectory(utils.getOutputDirectory());

		CarriersUtils.splitCarriers(
				scenario,
				CarrierSplitter.ClusteringStrategy.GREEDY,
				CarrierSplitter.ShipmentClusteringLocation.PICKUP,
				2);

		Path outputFile = Path.of(utils.getOutputDirectory()).resolve("carrier_split.png");
		assertTrue(Files.exists(outputFile), "splitCarriers should write a PNG plot to the output directory.");
		assertTrue(outputFile.toFile().length() > 0, "The PNG plot should not be empty.");
	}

	@Test
	void writesCarrierSplitPlotNextToConfiguredCarrierFile() throws Exception {
		Scenario scenario = createShipmentScenario();
		Path carrierDirectory = Path.of(utils.getOutputDirectory()).resolve("generatedInputData");
		Files.createDirectories(carrierDirectory);
		ConfigUtils.addOrGetModule(scenario.getConfig(), FreightCarriersConfigGroup.class)
				.setCarriersFile(carrierDirectory.resolve("input_carriers.xml.gz").toString());

		CarriersUtils.splitCarriers(
				scenario,
				CarrierSplitter.ClusteringStrategy.GREEDY,
				CarrierSplitter.ShipmentClusteringLocation.PICKUP,
				2);

		Path outputFile = carrierDirectory.resolve("carrier_split.png");
		assertTrue(Files.exists(outputFile), "splitCarriers should write the PNG plot next to the configured carrier file.");
		assertTrue(outputFile.toFile().length() > 0, "The PNG plot should not be empty.");
	}

	@Test
	void leavesCarrierUnchangedWhenSplitIsNotRequired() {
		Scenario scenario = createShipmentScenario();
		Carriers carriers = CarriersUtils.getCarriers(scenario);
		Carrier originalCarrier = carriers.getCarriers().values().iterator().next();
		Id<Carrier> originalCarrierId = originalCarrier.getId();
		int originalShipmentCount = originalCarrier.getShipments().size();
		int originalJspritIterations = CarriersUtils.getJspritIterations(originalCarrier);

		CarriersUtils.splitCarriers(scenario, CarrierSplitter.ClusteringStrategy.RANDOM, null, 10);

		Carriers splitCarriers = CarriersUtils.getCarriers(scenario);
		assertEquals(1, splitCarriers.getCarriers().size(), "Carrier should not be split when the job limit is not exceeded.");
		Carrier unchangedCarrier = splitCarriers.getCarriers().get(originalCarrierId);
		assertNotNull(unchangedCarrier, "Original carrier id should be preserved when no split is required.");
		assertEquals(originalShipmentCount, unchangedCarrier.getShipments().size(), "All shipments should stay on the original carrier.");
		assertEquals(originalJspritIterations, CarriersUtils.getJspritIterations(unchangedCarrier), "Original jsprit iterations should remain unchanged.");
		assertEquals(ORIGINAL_CARRIER_ATTRIBUTE_VALUE, unchangedCarrier.getAttributes().getAttribute(ORIGINAL_CARRIER_ATTRIBUTE));
	}

	@Test
	void createsDeterministicServiceSplitsIndependentOfInputOrder() {
		for (CarrierSplitter.ClusteringStrategy strategy : CarrierSplitter.ClusteringStrategy.values()) {
			Scenario firstScenario = createServiceScenario(false);
			Scenario secondScenario = createServiceScenario(true);

			CarriersUtils.splitCarriers(firstScenario, strategy, null, 10);
			CarriersUtils.splitCarriers(secondScenario, strategy, null, 10);

			assertEquals(
					splitSignature(CarriersUtils.getCarriers(firstScenario)),
					splitSignature(CarriersUtils.getCarriers(secondScenario)),
					"Split should be deterministic for " + strategy);
		}
	}

	@Test
	void greedySplitDoesNotCreateEmptyRemainderCarrier() {
		Scenario scenario = createServiceScenario(62);

		CarriersUtils.splitCarriers(scenario, CarrierSplitter.ClusteringStrategy.GREEDY, null, 10);

		Carriers splitCarriers = CarriersUtils.getCarriers(scenario);
		assertEquals(6, splitCarriers.getCarriers().size(), "62 services may be split into six non-empty carriers when the remainder is bundled.");
		assertEquals(62, countServices(splitCarriers), "All services must remain assigned after splitting.");
		for (Carrier splitCarrier : splitCarriers.getCarriers().values()) {
			assertTrue(splitCarrier.getServices().size() <= 13, "Greedy remainder clusters should stay within the 1.3 tolerance.");
			assertFalse(splitCarrier.getServices().isEmpty(), "Greedy split should not create empty remainder carriers.");
		}
	}

	private void assertShipmentCarrierSplit(CarrierSplitter.ClusteringStrategy strategy) {
		Scenario scenario = createShipmentScenario();
		Carriers carriers = CarriersUtils.getCarriers(scenario);
		Carrier originalCarrier = carriers.getCarriers().values().iterator().next();
		int originalShipmentCount = originalCarrier.getShipments().size();
		int originalJspritIterations = CarriersUtils.getJspritIterations(originalCarrier);

		CarriersUtils.splitCarriers(
				scenario,
				strategy,
				CarrierSplitter.ShipmentClusteringLocation.DELIVERY,
				2);

		Carriers splitCarriers = CarriersUtils.getCarriers(scenario);
		assertEquals(3, splitCarriers.getCarriers().size(), "Five shipments with max two jobs should create three carriers.");
		assertEquals(originalShipmentCount, countShipments(splitCarriers), "All original shipments must remain assigned after splitting.");
		assertEquals(0, countServices(splitCarriers), "Shipment carrier split must not create services.");

		for (Carrier splitCarrier : splitCarriers.getCarriers().values()) {
			assertTrue(splitCarrier.getShipments().size() <= 2, "No split carrier should exceed maxJobsPerCarrier.");
			assertEquals(originalJspritIterations, CarriersUtils.getJspritIterations(splitCarrier), "Split carriers must inherit jsprit iterations.");
			assertEquals(ORIGINAL_CARRIER_ATTRIBUTE_VALUE, splitCarrier.getAttributes().getAttribute(ORIGINAL_CARRIER_ATTRIBUTE));
			assertSplitVehicles(originalCarrier, splitCarrier);
			assertEquals(CarrierCapabilities.FleetSize.INFINITE, splitCarrier.getCarrierCapabilities().getFleetSize());
			assertTrue(splitCarrier.getServices().isEmpty(), "Shipment split carriers must not contain services.");
		}
		assertVehicleIdsAreUniqueAcrossCarriers(splitCarriers);
	}

	private void assertServiceCarrierSplit(CarrierSplitter.ClusteringStrategy strategy) {
		Scenario scenario = createServiceScenario();
		Carriers carriers = CarriersUtils.getCarriers(scenario);
		int originalServiceCount = countServices(carriers);
		Set<Integer> originalJspritIterations = new HashSet<>();
		Carrier originalCarrier = carriers.getCarriers().values().iterator().next();
		for (Carrier carrier : carriers.getCarriers().values()) {
			originalJspritIterations.add(CarriersUtils.getJspritIterations(carrier));
		}

		CarriersUtils.splitCarriers(scenario, strategy, null, 10);

		Carriers splitCarriers = CarriersUtils.getCarriers(scenario);
		assertTrue(splitCarriers.getCarriers().size() >= 2, "The service carrier should be split into at least the estimated carriers.");
		assertEquals(originalServiceCount, countServices(splitCarriers), "All original services must remain assigned after splitting.");
		assertEquals(0, countShipments(splitCarriers), "Service carrier split must not create shipments.");

		for (Carrier splitCarrier : splitCarriers.getCarriers().values()) {
			assertTrue(splitCarrier.getServices().size() <= 13, "Service clusters should stay close to the requested size.");
			assertTrue(originalJspritIterations.contains(CarriersUtils.getJspritIterations(splitCarrier)), "Split carriers must inherit jsprit iterations.");
			assertEquals(ORIGINAL_CARRIER_ATTRIBUTE_VALUE, splitCarrier.getAttributes().getAttribute(ORIGINAL_CARRIER_ATTRIBUTE));
			assertSplitVehicles(originalCarrier, splitCarrier);
			assertEquals(CarrierCapabilities.FleetSize.INFINITE, splitCarrier.getCarrierCapabilities().getFleetSize());
			assertTrue(splitCarrier.getShipments().isEmpty(), "Service split carriers must not contain shipments.");
		}
		assertVehicleIdsAreUniqueAcrossCarriers(splitCarriers);
	}

	private Scenario createShipmentScenario() {
		Scenario scenario = createChessboardScenario();

		Carriers carriers = CarriersUtils.addOrGetCarriers(scenario);
		Carrier carrier = createCarrier("shipmentCarrier", 100);
		for (int i = 0; i < 5; i++) {
			String pickupLinkId = CHESSBOARD_LINK_IDS[i];
			String deliveryLinkId = CHESSBOARD_LINK_IDS[i + 5];

			CarrierShipment shipment = CarrierShipment.Builder.newInstance(
					Id.create("shipment" + i, CarrierShipment.class),
					Id.createLinkId(pickupLinkId),
					Id.createLinkId(deliveryLinkId),
					1).build();
			CarriersUtils.addShipment(carrier, shipment);
		}
		carriers.addCarrier(carrier);
		return scenario;
	}

	private Scenario createServiceScenario() {
		return createServiceScenario(false);
	}

	private Scenario createServiceScenario(boolean reverseServiceInsertionOrder) {
		return createServiceScenario(20, reverseServiceInsertionOrder);
	}

	private Scenario createServiceScenario(int numberOfServices) {
		return createServiceScenario(numberOfServices, false);
	}

	private Scenario createServiceScenario(int numberOfServices, boolean reverseServiceInsertionOrder) {
		Scenario scenario = createChessboardScenario();
		Carriers carriers = CarriersUtils.addOrGetCarriers(scenario);
		Carrier carrier = createCarrier("serviceCarrier", 75);
		List<Integer> serviceIds = new ArrayList<>();
		for (int i = 0; i < numberOfServices; i++) {
			serviceIds.add(i);
		}
		if (reverseServiceInsertionOrder) {
			Collections.reverse(serviceIds);
		}
		for (int i : serviceIds) {
			String serviceLinkId = CHESSBOARD_LINK_IDS[i % CHESSBOARD_LINK_IDS.length];

			CarrierService service = CarrierService.Builder.newInstance(
							Id.create("service" + i, CarrierService.class),
							Id.createLinkId(serviceLinkId),
							1)
					.setServiceDuration(60)
					.build();
			CarriersUtils.addService(carrier, service);
		}
		carriers.addCarrier(carrier);
		return scenario;
	}

	private Scenario createChessboardScenario() {
		var scenarioUrl = ExamplesUtils.getTestScenarioURL("freight-chessboard-9x9");
		var config = ConfigUtils.createConfig();
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.network().setInputFile(IOUtils.extendUrl(scenarioUrl, "grid9x9.xml").toString());
		return ScenarioUtils.loadScenario(config);
	}

	private static Carrier createCarrier(String carrierId, int jspritIterations) {
		Carrier carrier = CarriersUtils.createCarrier(Id.create(carrierId, Carrier.class));
		carrier.getAttributes().putAttribute(ORIGINAL_CARRIER_ATTRIBUTE, ORIGINAL_CARRIER_ATTRIBUTE_VALUE);
		carrier.setCarrierCapabilities(createCarrierCapabilities(carrierId, CarrierSplitterTest.DEPOT_LINK_ID));
		CarriersUtils.setJspritIterations(carrier, jspritIterations);
		return carrier;
	}

	private static CarrierCapabilities createCarrierCapabilities(String carrierId, String secondVehicleLinkId) {
		VehicleType vehicleType = VehicleUtils.createDefaultVehicleType();
		CarrierVehicle firstVehicle = CarrierVehicle.newInstance(
				Id.create(carrierId + "_vehicle", Vehicle.class),
				Id.createLinkId(CarrierSplitterTest.DEPOT_LINK_ID),
				vehicleType);
		firstVehicle.getAttributes().putAttribute(ORIGINAL_VEHICLE_ATTRIBUTE, ORIGINAL_VEHICLE_ATTRIBUTE_VALUE);
		CarrierVehicle secondVehicle = CarrierVehicle.newInstance(
				Id.create(carrierId + "_secondVehicle", Vehicle.class),
				Id.createLinkId(secondVehicleLinkId),
				vehicleType);
		secondVehicle.getAttributes().putAttribute(ORIGINAL_VEHICLE_ATTRIBUTE, ORIGINAL_VEHICLE_ATTRIBUTE_VALUE);
		return CarrierCapabilities.Builder.newInstance()
				.addVehicle(firstVehicle)
				.addVehicle(secondVehicle)
				.setFleetSize(CarrierCapabilities.FleetSize.INFINITE)
				.build();
	}

	private static int countShipments(Carriers carriers) {
		return carriers.getCarriers().values().stream().mapToInt(carrier -> carrier.getShipments().size()).sum();
	}

	private static int countServices(Carriers carriers) {
		return carriers.getCarriers().values().stream().mapToInt(carrier -> carrier.getServices().size()).sum();
	}

	private static void assertSplitVehicles(Carrier originalCarrier, Carrier splitCarrier) {
		List<CarrierVehicle> originalVehicles = originalCarrier.getCarrierCapabilities().getCarrierVehicles().values().stream()
				.sorted(Comparator.comparing(left -> left.getId().toString()))
				.toList();
		List<CarrierVehicle> splitVehicles = splitCarrier.getCarrierCapabilities().getCarrierVehicles().values().stream()
				.sorted(Comparator.comparing(left -> left.getId().toString()))
				.toList();
		assertEquals(originalVehicles.size(), splitVehicles.size(), "Split carriers must inherit all vehicles from the original infinite fleet.");
		for (int i = 0; i < originalVehicles.size(); i++) {
			CarrierVehicle originalVehicle = originalVehicles.get(i);
			CarrierVehicle splitVehicle = splitVehicles.get(i);
			assertEquals(Id.createVehicleId(splitCarrier.getId() + "_" + (i + 1)),
					splitVehicle.getId(), "Split vehicle id should use the split carrier id as prefix.");
			assertEquals(originalVehicle.getLinkId(), splitVehicle.getLinkId(), "Split vehicle start link should match the original vehicle.");
			assertEquals(originalVehicle.getType(), splitVehicle.getType(), "Split vehicle type should match the original vehicle.");
			assertEquals(originalVehicle.getEarliestStartTime(), splitVehicle.getEarliestStartTime(), "Split vehicle earliest start should match the original vehicle.");
			assertEquals(originalVehicle.getLatestEndTime(), splitVehicle.getLatestEndTime(), "Split vehicle latest end should match the original vehicle.");
			assertEquals(ORIGINAL_VEHICLE_ATTRIBUTE_VALUE, splitVehicle.getAttributes().getAttribute(ORIGINAL_VEHICLE_ATTRIBUTE));
		}
	}

	private static void assertVehicleIdsAreUniqueAcrossCarriers(Carriers carriers) {
		List<Id<Vehicle>> vehicleIds = carriers.getCarriers().values().stream()
				.flatMap(carrier -> carrier.getCarrierCapabilities().getCarrierVehicles().keySet().stream())
				.toList();
		Set<Id<Vehicle>> uniqueVehicleIds = new HashSet<>(vehicleIds);
		assertEquals(vehicleIds.size(), uniqueVehicleIds.size(), "Vehicle IDs must be unique across all split carriers.");
		Set<String> carrierIds = carriers.getCarriers().values().stream()
				.map(carrier -> carrier.getId().toString())
				.collect(Collectors.toSet());
		assertTrue(vehicleIds.stream().map(Id::toString).noneMatch(carrierIds::contains), "Vehicle IDs must not be identical to carrier IDs.");
	}

	private static String splitSignature(Carriers carriers) {
		StringBuilder signature = new StringBuilder();
		carriers.getCarriers().values().stream()
				.sorted(Comparator.comparing(left -> left.getId().toString()))
				.forEach(carrier -> {
					signature.append(carrier.getId()).append(":");
					carrier.getShipments().values().stream()
							.map(shipment -> shipment.getId().toString())
							.sorted()
							.forEach(id -> signature.append("shipment=").append(id).append(";"));
					carrier.getServices().values().stream()
							.map(service -> service.getId().toString())
							.sorted()
							.forEach(id -> signature.append("service=").append(id).append(";"));
					signature.append("|");
				});
		return signature.toString();
	}
}
