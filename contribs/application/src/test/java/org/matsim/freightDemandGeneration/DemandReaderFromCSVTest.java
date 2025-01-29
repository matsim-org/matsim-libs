package org.matsim.freightDemandGeneration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.options.ShpOptions;
import org.matsim.freight.carriers.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.freightDemandGeneration.CarrierReaderFromCSV.CarrierInformationElement;
import org.matsim.freightDemandGeneration.DemandReaderFromCSV.DemandInformationElement;
import org.matsim.testcases.MatsimTestUtils;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/**
 * @author Ricardo Ewert
 *
 */
public class DemandReaderFromCSVTest {
	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testLinkForPerson() {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(
				"https://raw.githubusercontent.com/matsim-org/matsim-libs/master/examples/scenarios/freight-chessboard-9x9/grid9x9.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		String populationLocation = utils.getPackageInputDirectory() + "testPopulation.xml";
		Population population = PopulationUtils.readPopulation(populationLocation);
		FreightDemandGenerationUtils.preparePopulation(population, 1.0, 1.0, "changeNumberOfLocationsWithDemand");
		HashMap<Id<Person>, TreeMap<Double, String>> nearestLinkPerPerson = new HashMap<>();
		for (Person person :  population.getPersons().values()) {
			DemandReaderFromCSV.findLinksForPerson(scenario, nearestLinkPerPerson, person);
		}
		Assertions.assertEquals("j(1,8)",nearestLinkPerPerson.get(Id.createPersonId("person1")).values().iterator().next());
		Assertions.assertEquals("j(3,3)",nearestLinkPerPerson.get(Id.createPersonId("person2")).values().iterator().next());
		Assertions.assertEquals("j(4,5)R",nearestLinkPerPerson.get(Id.createPersonId("person3")).values().iterator().next());
		Assertions.assertEquals("j(5,3)",nearestLinkPerPerson.get(Id.createPersonId("person4")).values().iterator().next());
		Assertions.assertEquals("j(5,6)",nearestLinkPerPerson.get(Id.createPersonId("person5")).values().iterator().next());
		Assertions.assertEquals("j(8,8)R",nearestLinkPerPerson.get(Id.createPersonId("person6")).values().iterator().next());
		Assertions.assertEquals("i(5,9)R",nearestLinkPerPerson.get(Id.createPersonId("person7")).values().iterator().next());
		Assertions.assertEquals("i(9,5)R",nearestLinkPerPerson.get(Id.createPersonId("person8")).values().iterator().next());
	}

	@Test
	void demandCreationWithSampleWithChangeNumberOfLocations() throws IOException {
		// read inputs
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(
				"https://raw.githubusercontent.com/matsim-org/matsim-libs/master/examples/scenarios/freight-chessboard-9x9/grid9x9.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		FreightCarriersConfigGroup freightCarriersConfigGroup = ConfigUtils.addOrGetModule(scenario.getConfig(),
				FreightCarriersConfigGroup.class);
		freightCarriersConfigGroup.setCarriersVehicleTypesFile(utils.getPackageInputDirectory() + "testVehicleTypes.xml");
		Path carrierCSVLocation = Path.of(utils.getPackageInputDirectory() + "testCarrierCSV.csv");
		Path demandCSVLocation = Path.of(utils.getPackageInputDirectory() + "testDemandCSV.csv");
		Path shapeFilePath = Path.of(utils.getPackageInputDirectory() + "testShape/testShape.shp");
		ShpOptions shp = new ShpOptions(shapeFilePath, "WGS84", null);
		String shapeCategory = "Ortsteil";
		ShpOptions.Index indexShape = shp.createIndex("Ortsteil");
		String populationLocation = utils.getPackageInputDirectory() + "testPopulation.xml";
		Population population = PopulationUtils.readPopulation(populationLocation);
		FreightDemandGenerationUtils.preparePopulation(population, 0.5, 1.0, "changeNumberOfLocationsWithDemand");
		Boolean combineSimilarJobs = false;


		// run methods
		createDemandAndCheckCarrier(carrierCSVLocation, scenario, freightCarriersConfigGroup, indexShape, demandCSVLocation, shapeCategory,
			population, combineSimilarJobs);

		Network network = scenario.getNetwork();

		checkCarrier1and2(scenario, network, indexShape);

		Object2IntMap<Integer> countShipmentsWithCertainDemand;
		Map<String, Set<String>> locationsPerShipmentElement;
		int countDemand;

		// check carrier 3
		Carrier testCarrier3 = CarriersUtils.getCarriers(scenario).getCarriers()
				.get(Id.create("testCarrier3", Carrier.class));
		Assertions.assertEquals(0, testCarrier3.getServices().size());
		Assertions.assertEquals(4, testCarrier3.getShipments().size());
		countShipmentsWithCertainDemand = new Object2IntOpenHashMap<>();
		locationsPerShipmentElement = new HashMap<>();
		countDemand = 0;
		for (CarrierShipment shipment : testCarrier3.getShipments().values()) {
            countShipmentsWithCertainDemand.merge((Integer) shipment.getCapacityDemand(), 1, Integer::sum);
            countDemand = countDemand + shipment.getCapacityDemand();
            Assertions.assertEquals(5, shipment.getCapacityDemand());
			Assertions.assertEquals(2000, shipment.getPickupDuration(), MatsimTestUtils.EPSILON);
			Assertions.assertEquals(1250, shipment.getDeliveryDuration(), MatsimTestUtils.EPSILON);
			Assertions.assertEquals(TimeWindow.newInstance(8000, 50000), shipment.getPickupStartingTimeWindow());
			Assertions.assertEquals(TimeWindow.newInstance(10000, 60000), shipment.getDeliveryStartingTimeWindow());
			locationsPerShipmentElement.computeIfAbsent("ShipmentElement1_pickup", (k) -> new HashSet<>())
					.add(shipment.getPickupLinkId().toString());
			locationsPerShipmentElement.computeIfAbsent("ShipmentElement1_delivery", (k) -> new HashSet<>())
					.add(shipment.getDeliveryLinkId().toString());
		}
		Assertions.assertEquals(20, countDemand);
		Assertions.assertEquals(4, countShipmentsWithCertainDemand.getInt(5));
		Assertions.assertEquals(2, locationsPerShipmentElement.get("ShipmentElement1_pickup").size());
		Assertions.assertEquals(4, locationsPerShipmentElement.get("ShipmentElement1_delivery").size());
		for (String locationsOfShipmentElement : locationsPerShipmentElement.get("ShipmentElement1_delivery")) {
			Link link = network.getLinks().get(Id.createLinkId(locationsOfShipmentElement));
			Assertions.assertTrue(
					FreightDemandGenerationUtils.checkPositionInShape(link, null, indexShape, null, null));
			Assertions.assertTrue(FreightDemandGenerationUtils.checkPositionInShape(link, null, indexShape,
					new String[] { "area1" }, null));
			Assertions.assertFalse(FreightDemandGenerationUtils.checkPositionInShape(link, null, indexShape,
					new String[] { "area2" }, null));
		}
	}

	@Test
	void demandCreationWithSampleWithDemandOnLocation() throws IOException {
		// read inputs
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(
			"https://raw.githubusercontent.com/matsim-org/matsim-libs/master/examples/scenarios/freight-chessboard-9x9/grid9x9.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		FreightCarriersConfigGroup freightCarriersConfigGroup = ConfigUtils.addOrGetModule(scenario.getConfig(),
			FreightCarriersConfigGroup.class);
		freightCarriersConfigGroup.setCarriersVehicleTypesFile(utils.getPackageInputDirectory() + "testVehicleTypes.xml");
		Path carrierCSVLocation = Path.of(utils.getPackageInputDirectory() + "testCarrierCSV.csv");
		Path demandCSVLocation = Path.of(utils.getPackageInputDirectory() + "testDemandCSV.csv");
		Path shapeFilePath = Path.of(utils.getPackageInputDirectory() + "testShape/testShape.shp");
		ShpOptions shp = new ShpOptions(shapeFilePath, "WGS84", null);
		String shapeCategory = "Ortsteil";
		ShpOptions.Index indexShape = shp.createIndex("Ortsteil");
		String populationLocation = utils.getPackageInputDirectory() + "testPopulation.xml";
		Population population = PopulationUtils.readPopulation(populationLocation);
		FreightDemandGenerationUtils.preparePopulation(population, 0.5, 1.0, "changeDemandOnLocation");
		Boolean combineSimilarJobs = false;

		createDemandAndCheckCarrier(carrierCSVLocation, scenario, freightCarriersConfigGroup, indexShape, demandCSVLocation, shapeCategory,
			population, combineSimilarJobs);

		Network network = scenario.getNetwork();

		checkCarrier1and2(scenario, network, indexShape);
		int countDemand;
		Object2IntMap<Integer> countShipmentsWithCertainDemand;
		Map<String, Set<String>> locationsPerShipmentElement;

		// check carrier 3
		Carrier testCarrier3 = CarriersUtils.getCarriers(scenario).getCarriers()
			.get(Id.create("testCarrier3", Carrier.class));
		Assertions.assertEquals(0, testCarrier3.getServices().size());
		Assertions.assertEquals(2, testCarrier3.getShipments().size());
		countShipmentsWithCertainDemand = new Object2IntOpenHashMap<>();
		locationsPerShipmentElement = new HashMap<>();
		countDemand = 0;
		for (CarrierShipment shipment : testCarrier3.getShipments().values()) {
            countShipmentsWithCertainDemand.merge((Integer) shipment.getCapacityDemand(), 1, Integer::sum);
            countDemand = countDemand + shipment.getCapacityDemand();
            Assertions.assertEquals(10, shipment.getCapacityDemand());
			Assertions.assertEquals(4000, shipment.getPickupDuration(), MatsimTestUtils.EPSILON);
			Assertions.assertEquals(2500, shipment.getDeliveryDuration(), MatsimTestUtils.EPSILON);
			Assertions.assertEquals(TimeWindow.newInstance(8000, 50000), shipment.getPickupStartingTimeWindow());
			Assertions.assertEquals(TimeWindow.newInstance(10000, 60000), shipment.getDeliveryStartingTimeWindow());
			locationsPerShipmentElement.computeIfAbsent("ShipmentElement1_pickup", (k) -> new HashSet<>())
				.add(shipment.getPickupLinkId().toString());
			locationsPerShipmentElement.computeIfAbsent("ShipmentElement1_delivery", (k) -> new HashSet<>())
				.add(shipment.getDeliveryLinkId().toString());
		}
		Assertions.assertEquals(20, countDemand);
		Assertions.assertEquals(2, countShipmentsWithCertainDemand.getInt(10));
		Assertions.assertEquals(1, locationsPerShipmentElement.get("ShipmentElement1_pickup").size());
		Assertions.assertEquals(2, locationsPerShipmentElement.get("ShipmentElement1_delivery").size());
		for (String locationsOfShipmentElement : locationsPerShipmentElement.get("ShipmentElement1_delivery")) {
			Link link = network.getLinks().get(Id.createLinkId(locationsOfShipmentElement));
			Assertions.assertTrue(
				FreightDemandGenerationUtils.checkPositionInShape(link, null, indexShape, null, null));
			Assertions.assertTrue(FreightDemandGenerationUtils.checkPositionInShape(link, null, indexShape,
				new String[] { "area1" }, null));
			Assertions.assertFalse(FreightDemandGenerationUtils.checkPositionInShape(link, null, indexShape,
				new String[] { "area2" }, null));
		}
	}

	@Test
	void demandCreationWithSampleWithDemandOnLocationWithCombiningJobs() throws IOException {
		// read inputs
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(
			"https://raw.githubusercontent.com/matsim-org/matsim-libs/master/examples/scenarios/freight-chessboard-9x9/grid9x9.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		FreightCarriersConfigGroup freightCarriersConfigGroup = ConfigUtils.addOrGetModule(scenario.getConfig(),
			FreightCarriersConfigGroup.class);
		freightCarriersConfigGroup.setCarriersVehicleTypesFile(utils.getPackageInputDirectory() + "testVehicleTypes.xml");
		Path carrierCSVLocation = Path.of(utils.getPackageInputDirectory() + "testCarrierCSV.csv");
		Path demandCSVLocation = Path.of(utils.getPackageInputDirectory() + "testDemandCSV.csv");
		Path shapeFilePath = Path.of(utils.getPackageInputDirectory() + "testShape/testShape.shp");
		ShpOptions shp = new ShpOptions(shapeFilePath, "WGS84", null);
		String shapeCategory = "Ortsteil";
		ShpOptions.Index indexShape = shp.createIndex("Ortsteil");
		String populationLocation = utils.getPackageInputDirectory() + "testPopulation.xml";
		Population population = PopulationUtils.readPopulation(populationLocation);
		FreightDemandGenerationUtils.preparePopulation(population, 0.5, 1.0, "changeDemandOnLocation");
		Boolean combineSimilarJobs = true;

		createDemandAndCheckCarrier(carrierCSVLocation, scenario, freightCarriersConfigGroup, indexShape, demandCSVLocation, shapeCategory,
			population, combineSimilarJobs);

		Network network = scenario.getNetwork();

		checkCarrier1and2WithCombiningJobs(scenario, network, indexShape);
		int countDemand;
		Object2IntMap<Integer> countShipmentsWithCertainDemand;
		Map<String, Set<String>> locationsPerShipmentElement;

		// check carrier 3
		Carrier testCarrier3 = CarriersUtils.getCarriers(scenario).getCarriers()
			.get(Id.create("testCarrier3", Carrier.class));
		Assertions.assertEquals(0, testCarrier3.getServices().size());
		Assertions.assertEquals(2, testCarrier3.getShipments().size());
		countShipmentsWithCertainDemand = new Object2IntOpenHashMap<>();
		locationsPerShipmentElement = new HashMap<>();
		countDemand = 0;
		for (CarrierShipment shipment : testCarrier3.getShipments().values()) {
            countShipmentsWithCertainDemand.merge((Integer) shipment.getCapacityDemand(), 1, Integer::sum);
            countDemand = countDemand + shipment.getCapacityDemand();
            Assertions.assertEquals(10, shipment.getCapacityDemand());
			Assertions.assertEquals(4000, shipment.getPickupDuration(), MatsimTestUtils.EPSILON);
			Assertions.assertEquals(2500, shipment.getDeliveryDuration(), MatsimTestUtils.EPSILON);
			Assertions.assertEquals(TimeWindow.newInstance(8000, 50000), shipment.getPickupStartingTimeWindow());
			Assertions.assertEquals(TimeWindow.newInstance(10000, 60000), shipment.getDeliveryStartingTimeWindow());
			locationsPerShipmentElement.computeIfAbsent("ShipmentElement1_pickup", (k) -> new HashSet<>())
				.add(shipment.getPickupLinkId().toString());
			locationsPerShipmentElement.computeIfAbsent("ShipmentElement1_delivery", (k) -> new HashSet<>())
				.add(shipment.getDeliveryLinkId().toString());
		}
		Assertions.assertEquals(20, countDemand);
		Assertions.assertEquals(2, countShipmentsWithCertainDemand.getInt(10));
		Assertions.assertEquals(1, locationsPerShipmentElement.get("ShipmentElement1_pickup").size());
		Assertions.assertEquals(2, locationsPerShipmentElement.get("ShipmentElement1_delivery").size());
		for (String locationsOfShipmentElement : locationsPerShipmentElement.get("ShipmentElement1_delivery")) {
			Link link = network.getLinks().get(Id.createLinkId(locationsOfShipmentElement));
			Assertions.assertTrue(
				FreightDemandGenerationUtils.checkPositionInShape(link, null, indexShape, null, null));
			Assertions.assertTrue(FreightDemandGenerationUtils.checkPositionInShape(link, null, indexShape,
				new String[] { "area1" }, null));
			Assertions.assertFalse(FreightDemandGenerationUtils.checkPositionInShape(link, null, indexShape,
				new String[] { "area2" }, null));
		}
	}


	@Test
	void demandCreationNoSampling() throws IOException {
		// read inputs
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(
			"https://raw.githubusercontent.com/matsim-org/matsim-libs/master/examples/scenarios/freight-chessboard-9x9/grid9x9.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		FreightCarriersConfigGroup freightCarriersConfigGroup = ConfigUtils.addOrGetModule(scenario.getConfig(),
			FreightCarriersConfigGroup.class);
		freightCarriersConfigGroup.setCarriersVehicleTypesFile(utils.getPackageInputDirectory() + "testVehicleTypes.xml");
		Path carrierCSVLocation = Path.of(utils.getPackageInputDirectory() + "testCarrierCSV.csv");
		Path demandCSVLocation = Path.of(utils.getPackageInputDirectory() + "testDemandCSV.csv");
		Path shapeFilePath = Path.of(utils.getPackageInputDirectory() + "testShape/testShape.shp");
		ShpOptions shp = new ShpOptions(shapeFilePath, "WGS84", null);
		String shapeCategory = "Ortsteil";
		ShpOptions.Index indexShape = shp.createIndex("Ortsteil");
		String populationLocation = utils.getPackageInputDirectory() + "testPopulation.xml";
		Population population = PopulationUtils.readPopulation(populationLocation);
		FreightDemandGenerationUtils.preparePopulation(population, 0.5, 0.5, "changeDemandOnLocation");
		Boolean combineSimilarJobs = false;

		// run methods
		createDemandAndCheckCarrier(carrierCSVLocation, scenario, freightCarriersConfigGroup, indexShape, demandCSVLocation, shapeCategory,
			population, combineSimilarJobs);

		// check carrier 1
		Network network = scenario.getNetwork();

		checkCarrier1and2(scenario, network, indexShape);
		Object2IntMap<Integer> countShipmentsWithCertainDemand;
		Map<String, Set<String>> locationsPerShipmentElement;
		int countDemand;

		// check carrier 3
		Carrier testCarrier3 = CarriersUtils.getCarriers(scenario).getCarriers()
			.get(Id.create("testCarrier3", Carrier.class));
		Assertions.assertEquals(0, testCarrier3.getServices().size());
		Assertions.assertEquals(2, testCarrier3.getShipments().size());
		countShipmentsWithCertainDemand = new Object2IntOpenHashMap<>();
		locationsPerShipmentElement = new HashMap<>();
		countDemand = 0;
		for (CarrierShipment shipment : testCarrier3.getShipments().values()) {
            countShipmentsWithCertainDemand.merge((Integer) shipment.getCapacityDemand(), 1, Integer::sum);
            countDemand = countDemand + shipment.getCapacityDemand();
            Assertions.assertEquals(10, shipment.getCapacityDemand());
			Assertions.assertEquals(4000, shipment.getPickupDuration(), MatsimTestUtils.EPSILON);
			Assertions.assertEquals(2500, shipment.getDeliveryDuration(), MatsimTestUtils.EPSILON);
			Assertions.assertEquals(TimeWindow.newInstance(8000, 50000), shipment.getPickupStartingTimeWindow());
			Assertions.assertEquals(TimeWindow.newInstance(10000, 60000), shipment.getDeliveryStartingTimeWindow());
			locationsPerShipmentElement.computeIfAbsent("ShipmentElement1_pickup", (k) -> new HashSet<>())
				.add(shipment.getPickupLinkId().toString());
			locationsPerShipmentElement.computeIfAbsent("ShipmentElement1_delivery", (k) -> new HashSet<>())
				.add(shipment.getDeliveryLinkId().toString());
		}
		Assertions.assertEquals(20, countDemand);
		Assertions.assertEquals(2, countShipmentsWithCertainDemand.getInt(10));
		Assertions.assertEquals(1, locationsPerShipmentElement.get("ShipmentElement1_pickup").size());
		Assertions.assertEquals(2, locationsPerShipmentElement.get("ShipmentElement1_delivery").size());
		for (String locationsOfShipmentElement : locationsPerShipmentElement.get("ShipmentElement1_delivery")) {
			Link link = network.getLinks().get(Id.createLinkId(locationsOfShipmentElement));
			Assertions.assertTrue(
				FreightDemandGenerationUtils.checkPositionInShape(link, null, indexShape, null, null));
			Assertions.assertTrue(FreightDemandGenerationUtils.checkPositionInShape(link, null, indexShape,
				new String[] { "area1" }, null));
			Assertions.assertFalse(FreightDemandGenerationUtils.checkPositionInShape(link, null, indexShape,
				new String[] { "area2" }, null));
		}
	}

	@Test
	void demandCreationParcelsNoSampling() throws IOException {
		// read inputs
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(
			"https://raw.githubusercontent.com/matsim-org/matsim-libs/master/examples/scenarios/freight-chessboard-9x9/grid9x9.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		FreightCarriersConfigGroup freightCarriersConfigGroup = ConfigUtils.addOrGetModule(scenario.getConfig(),
			FreightCarriersConfigGroup.class);
		freightCarriersConfigGroup.setCarriersVehicleTypesFile(utils.getPackageInputDirectory() + "testVehicleTypes.xml");
		Path carrierCSVLocation = Path.of(utils.getPackageInputDirectory() + "testCarrierCSV_parcels.csv");
		Path demandCSVLocation = Path.of(utils.getPackageInputDirectory() + "testDemandCSV_parcels.csv");
		Path shapeFilePath = Path.of(utils.getPackageInputDirectory() + "testShape/testShape.shp");
		ShpOptions shp = new ShpOptions(shapeFilePath, "WGS84", null);
		String shapeCategory = "Ortsteil";
		ShpOptions.Index indexShape = shp.createIndex("Ortsteil");
		String populationLocation = utils.getPackageInputDirectory() + "testPopulation.xml";
		Population population = PopulationUtils.readPopulation(populationLocation);
		FreightDemandGenerationUtils.preparePopulation(population, 0.5, 0.5, "changeDemandOnLocation");
		Boolean combineSimilarJobs = false;

		// run methods
		createDemandAndCheckCarrierForParcel(carrierCSVLocation, scenario, freightCarriersConfigGroup, indexShape, demandCSVLocation, shapeCategory,
			population, combineSimilarJobs);

		// check carrier 1
		Network network = scenario.getNetwork();

		Assertions.assertEquals(1, CarriersUtils.getCarriers(scenario).getCarriers().size());

		Carrier testCarrier1 = CarriersUtils.getCarriers(scenario).getCarriers()
			.get(Id.create("testCarrier1", Carrier.class));

		Assertions.assertEquals(3, testCarrier1.getShipments().values().stream().mapToInt(CarrierShipment::getSize).sum());
		Map<String, Set<String>> locationsPerShipmentElement = new HashMap<>();
		for (CarrierShipment shipment : testCarrier1.getShipments().values()) {
			Assertions.assertEquals("i(2,0)", shipment.getFrom().toString());
			Assertions.assertEquals(1, shipment.getSize());
			Assertions.assertEquals(0, shipment.getPickupServiceTime(), MatsimTestUtils.EPSILON);
			Assertions.assertEquals(180, shipment.getDeliveryServiceTime(), MatsimTestUtils.EPSILON);
			Assertions.assertEquals(TimeWindow.newInstance(25200, 64800), shipment.getPickupTimeWindow());
			Assertions.assertEquals(TimeWindow.newInstance(25200, 64800), shipment.getDeliveryTimeWindow());
			locationsPerShipmentElement.computeIfAbsent("ShipmentElement1_pickup", (k) -> new HashSet<>())
				.add(shipment.getFrom().toString());
			locationsPerShipmentElement.computeIfAbsent("ShipmentElement1_delivery", (k) -> new HashSet<>())
				.add(shipment.getTo().toString());
		}
		Assertions.assertEquals(1, locationsPerShipmentElement.get("ShipmentElement1_pickup").size());
		Assertions.assertEquals(3, locationsPerShipmentElement.get("ShipmentElement1_delivery").size());

		for (String locationsOfShipmentElement : locationsPerShipmentElement.get("ShipmentElement1_delivery")) {
			Link link = network.getLinks().get(Id.createLinkId(locationsOfShipmentElement));
			Assertions.assertTrue(
				FreightDemandGenerationUtils.checkPositionInShape(link, null, indexShape, null, null));
			Assertions.assertTrue(FreightDemandGenerationUtils.checkPositionInShape(link, null, indexShape,
				new String[] { "area1" }, null) || FreightDemandGenerationUtils.checkPositionInShape(link, null, indexShape,
				new String[] { "area2" }, null));
		}
	}

	@Test
	void csvDemandReader() throws IOException {

		Path demandCSVLocation = Path.of(utils.getPackageInputDirectory() + "testDemandCSV.csv");
		Set<DemandInformationElement> demandInformation = DemandReaderFromCSV.readDemandInformation(demandCSVLocation);
		Assertions.assertEquals(5, demandInformation.size());

		for (DemandInformationElement demandInformationElement : demandInformation) {
			if (demandInformationElement.getCarrierName().equals("testCarrier1")
					&& demandInformationElement.getNumberOfJobs() == 4) {
				Assertions.assertEquals(0, (int) demandInformationElement.getDemandToDistribute());
				Assertions.assertNull(demandInformationElement.getShareOfPopulationWithFirstJobElement());
				Assertions.assertEquals(1, demandInformationElement.getAreasFirstJobElement().length);
				Assertions.assertEquals("area2", demandInformationElement.getAreasFirstJobElement()[0]);
				Assertions.assertNull(demandInformationElement.getNumberOfFirstJobElementLocations());
				Assertions.assertNull(demandInformationElement.getLocationsOfFirstJobElement());
				Assertions.assertEquals(180, (int) demandInformationElement.getFirstJobElementTimePerUnit());
				Assertions.assertEquals(TimeWindow.newInstance(3000, 13000),
						demandInformationElement.getFirstJobElementTimeWindow());
				Assertions.assertNull(demandInformationElement.getShareOfPopulationWithSecondJobElement());
				Assertions.assertNull(demandInformationElement.getNumberOfSecondJobElementLocations());
				Assertions.assertNull(demandInformationElement.getLocationsOfSecondJobElement());
				Assertions.assertNull(demandInformationElement.getSecondJobElementTimePerUnit());
				Assertions.assertNull(demandInformationElement.getSecondJobElementTimeWindow());
			} else if (demandInformationElement.getCarrierName().equals("testCarrier1")
					&& demandInformationElement.getNumberOfJobs() == 10) {
				Assertions.assertEquals(12, (int) demandInformationElement.getDemandToDistribute());
				Assertions.assertNull(demandInformationElement.getShareOfPopulationWithFirstJobElement());
				Assertions.assertNull(demandInformationElement.getAreasFirstJobElement());
				Assertions.assertEquals(4, (int) demandInformationElement.getNumberOfFirstJobElementLocations());
				Assertions.assertEquals(1, demandInformationElement.getLocationsOfFirstJobElement().length);
				Assertions.assertEquals("i(2,0)", demandInformationElement.getLocationsOfFirstJobElement()[0]);
				Assertions.assertEquals(100, (int) demandInformationElement.getFirstJobElementTimePerUnit());
				Assertions.assertEquals(TimeWindow.newInstance(5000, 20000),
						demandInformationElement.getFirstJobElementTimeWindow());
				Assertions.assertNull(demandInformationElement.getShareOfPopulationWithSecondJobElement());
				Assertions.assertNull(demandInformationElement.getAreasSecondJobElement());
				Assertions.assertNull(demandInformationElement.getNumberOfSecondJobElementLocations());
				Assertions.assertNull(demandInformationElement.getLocationsOfSecondJobElement());
				Assertions.assertNull(demandInformationElement.getSecondJobElementTimePerUnit());
				Assertions.assertNull(demandInformationElement.getSecondJobElementTimeWindow());
			} else if (demandInformationElement.getCarrierName().equals("testCarrier2")
					&& demandInformationElement.getDemandToDistribute() == 0) {
				Assertions.assertEquals(0, (int) demandInformationElement.getDemandToDistribute());
				Assertions.assertEquals(4, (int) demandInformationElement.getNumberOfJobs());
				Assertions.assertNull(demandInformationElement.getShareOfPopulationWithFirstJobElement());
				Assertions.assertNull(demandInformationElement.getAreasFirstJobElement());
				Assertions.assertNull(demandInformationElement.getNumberOfFirstJobElementLocations());
				Assertions.assertNull(demandInformationElement.getLocationsOfFirstJobElement());
				Assertions.assertEquals(300, (int) demandInformationElement.getFirstJobElementTimePerUnit());
				Assertions.assertEquals(TimeWindow.newInstance(10000, 45000),
						demandInformationElement.getFirstJobElementTimeWindow());
				Assertions.assertNull(demandInformationElement.getShareOfPopulationWithSecondJobElement());
				Assertions.assertNull(demandInformationElement.getAreasSecondJobElement());
				Assertions.assertEquals(1, (int) demandInformationElement.getNumberOfSecondJobElementLocations());
				Assertions.assertEquals(1, demandInformationElement.getLocationsOfSecondJobElement().length);
				Assertions.assertEquals("i(2,0)", demandInformationElement.getLocationsOfSecondJobElement()[0]);
				Assertions.assertEquals(350, (int) demandInformationElement.getSecondJobElementTimePerUnit());
				Assertions.assertEquals(TimeWindow.newInstance(11000, 44000),
						demandInformationElement.getSecondJobElementTimeWindow());
			} else if (demandInformationElement.getCarrierName().equals("testCarrier2")
					&& demandInformationElement.getDemandToDistribute() == 15) {
				Assertions.assertEquals(15, (int) demandInformationElement.getDemandToDistribute());
				Assertions.assertEquals(7, (int) demandInformationElement.getNumberOfJobs());
				Assertions.assertNull(demandInformationElement.getShareOfPopulationWithFirstJobElement());
				Assertions.assertNull(demandInformationElement.getAreasFirstJobElement());
				Assertions.assertEquals(1, (int) demandInformationElement.getNumberOfFirstJobElementLocations());
				Assertions.assertNull(demandInformationElement.getLocationsOfFirstJobElement());
				Assertions.assertEquals(200, (int) demandInformationElement.getFirstJobElementTimePerUnit());
				Assertions.assertEquals(TimeWindow.newInstance(11000, 44000),
						demandInformationElement.getFirstJobElementTimeWindow());
				Assertions.assertNull(demandInformationElement.getShareOfPopulationWithSecondJobElement());
				Assertions.assertNull(demandInformationElement.getAreasSecondJobElement());
				Assertions.assertEquals(2, (int) demandInformationElement.getNumberOfSecondJobElementLocations());
				Assertions.assertNull(demandInformationElement.getLocationsOfSecondJobElement());
				Assertions.assertEquals(200, (int) demandInformationElement.getSecondJobElementTimePerUnit());
				Assertions.assertEquals(TimeWindow.newInstance(20000, 40000),
						demandInformationElement.getSecondJobElementTimeWindow());
			} else if (demandInformationElement.getCarrierName().equals("testCarrier3")) {
				Assertions.assertEquals(20, (int) demandInformationElement.getDemandToDistribute());
				Assertions.assertNull(demandInformationElement.getNumberOfJobs());
				Assertions.assertEquals(0.125, demandInformationElement.getShareOfPopulationWithFirstJobElement(),
						MatsimTestUtils.EPSILON);
				Assertions.assertNull(demandInformationElement.getAreasFirstJobElement());
				Assertions.assertNull(demandInformationElement.getNumberOfFirstJobElementLocations());
				Assertions.assertNull(demandInformationElement.getLocationsOfFirstJobElement());
				Assertions.assertEquals(400, (int) demandInformationElement.getFirstJobElementTimePerUnit());
				Assertions.assertEquals(TimeWindow.newInstance(8000, 50000),
						demandInformationElement.getFirstJobElementTimeWindow());
				Assertions.assertEquals(0.4, demandInformationElement.getShareOfPopulationWithSecondJobElement(),
						MatsimTestUtils.EPSILON);
				Assertions.assertEquals(1, demandInformationElement.getAreasSecondJobElement().length);
				Assertions.assertEquals("area1", demandInformationElement.getAreasSecondJobElement()[0]);
				Assertions.assertNull(demandInformationElement.getNumberOfSecondJobElementLocations());
				Assertions.assertNull(demandInformationElement.getLocationsOfSecondJobElement());
				Assertions.assertEquals(250, (int) demandInformationElement.getSecondJobElementTimePerUnit());
				Assertions.assertEquals(TimeWindow.newInstance(10000, 60000),
						demandInformationElement.getSecondJobElementTimeWindow());
			} else
				Assertions.fail("No expected demandInformationElement found");
		}
	}

	private static void createDemandAndCheckCarrier(Path carrierCSVLocation, Scenario scenario, FreightCarriersConfigGroup freightCarriersConfigGroup,
													ShpOptions.Index indexShape, Path demandCSVLocation, String shapeCategory,
													Population population, Boolean combineSimilarJobs) throws IOException {

		DemandGenerationSpecification demandGenerationSpecification = new DefaultDemandGenerationSpecification();
		// run methods
		Set<CarrierInformationElement> allNewCarrierInformation = CarrierReaderFromCSV
			.readCarrierInformation(carrierCSVLocation);
		CarrierReaderFromCSV.createNewCarrierAndAddVehicleTypes(scenario, allNewCarrierInformation, freightCarriersConfigGroup,
			indexShape, 1, null);
		Set<DemandInformationElement> demandInformation = DemandReaderFromCSV.readDemandInformation(demandCSVLocation);
		DemandReaderFromCSV.checkNewDemand(scenario, demandInformation, indexShape, shapeCategory);
		DemandReaderFromCSV.createDemandForCarriers(scenario, indexShape, demandInformation, population, combineSimilarJobs,
			null, demandGenerationSpecification);
		Assertions.assertEquals(3, CarriersUtils.getCarriers(scenario).getCarriers().size());
		Assertions.assertTrue(
			CarriersUtils.getCarriers(scenario).getCarriers().containsKey(Id.create("testCarrier1", Carrier.class)));
		Assertions.assertTrue(
			CarriersUtils.getCarriers(scenario).getCarriers().containsKey(Id.create("testCarrier2", Carrier.class)));
		Assertions.assertTrue(
			CarriersUtils.getCarriers(scenario).getCarriers().containsKey(Id.create("testCarrier3", Carrier.class)));
	}

	private static void createDemandAndCheckCarrierForParcel(Path carrierCSVLocation, Scenario scenario, FreightCarriersConfigGroup freightCarriersConfigGroup,
													ShpOptions.Index indexShape, Path demandCSVLocation, String shapeCategory,
													Population population, Boolean combineSimilarJobs) throws IOException {

		DemandGenerationSpecification demandGenerationSpecification = new DemandGenerationSpecificationForParcelDelivery(0.5, 2.0, true);
		// run methods
		Set<CarrierInformationElement> allNewCarrierInformation = CarrierReaderFromCSV
			.readCarrierInformation(carrierCSVLocation);
		CarrierReaderFromCSV.createNewCarrierAndAddVehicleTypes(scenario, allNewCarrierInformation, freightCarriersConfigGroup,
			indexShape, 1, null);
		Set<DemandInformationElement> demandInformation = DemandReaderFromCSV.readDemandInformation(demandCSVLocation);
		DemandReaderFromCSV.checkNewDemand(scenario, demandInformation, indexShape, shapeCategory);
		DemandReaderFromCSV.createDemandForCarriers(scenario, indexShape, demandInformation, population, combineSimilarJobs,
			null, demandGenerationSpecification);
		Assertions.assertEquals(1, CarriersUtils.getCarriers(scenario).getCarriers().size());
		Assertions.assertTrue(
			CarriersUtils.getCarriers(scenario).getCarriers().containsKey(Id.create("testCarrier1", Carrier.class)));
	}

	/**
	 * These results should be the same for these carriers.
	 * The difference is only based on the population sample methods for carrier3, because a shareOfThePopulation is used.
	 *
	 * @param scenario   the scenario
	 * @param network    the network
	 * @param indexShape the index of the shape
	 */
	private static void checkCarrier1and2(Scenario scenario, Network network, ShpOptions.Index indexShape) {
		Carrier testCarrier1 = CarriersUtils.getCarriers(scenario).getCarriers()
			.get(Id.create("testCarrier1", Carrier.class));
		Assertions.assertEquals(14, testCarrier1.getServices().size());
		Assertions.assertEquals(0, testCarrier1.getShipments().size());
		Object2IntMap<Integer> countServicesWithCertainDemand = new Object2IntOpenHashMap<>();
		Map<String, Set<String>> locationsPerServiceElement = new HashMap<>();
		int countDemand = 0;
		for (CarrierService service : testCarrier1.getServices().values()) {
			countServicesWithCertainDemand.merge((Integer) service.getCapacityDemand(), 1, Integer::sum);
			countDemand = countDemand + service.getCapacityDemand();
			if (service.getCapacityDemand() == 0) {
				Assertions.assertEquals(180, service.getServiceDuration(), MatsimTestUtils.EPSILON);
				Assertions.assertEquals(TimeWindow.newInstance(3000, 13000), service.getServiceStaringTimeWindow());
				locationsPerServiceElement.computeIfAbsent("serviceElement1", (k) -> new HashSet<>())
					.add(service.getServiceLinkId().toString());
			} else if (service.getCapacityDemand() == 1) {
				Assertions.assertEquals(100, service.getServiceDuration(), MatsimTestUtils.EPSILON);
				Assertions.assertEquals(TimeWindow.newInstance(5000, 20000), service.getServiceStaringTimeWindow());
				locationsPerServiceElement.computeIfAbsent("serviceElement2", (k) -> new HashSet<>())
					.add(service.getServiceLinkId().toString());
			} else {
				if (service.getCapacityDemand() == 2) {
					Assertions.assertEquals(200, service.getServiceDuration(), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(TimeWindow.newInstance(5000, 20000), service.getServiceStaringTimeWindow());
					locationsPerServiceElement.computeIfAbsent("serviceElement2", (k) -> new HashSet<>())
						.add(service.getServiceLinkId().toString());
				} else
					Assertions.fail("Service has a wrong demand.");
			}
		}
		Assertions.assertEquals(12, countDemand);
		Assertions.assertEquals(4, countServicesWithCertainDemand.getInt(0));
		Assertions.assertEquals(8, countServicesWithCertainDemand.getInt(1));
		Assertions.assertEquals(2, countServicesWithCertainDemand.getInt(2));
		Assertions.assertEquals(4, locationsPerServiceElement.get("serviceElement1").size());
		for (String locationsOfServiceElement : locationsPerServiceElement.get("serviceElement1")) {
			Link link = network.getLinks().get(Id.createLinkId(locationsOfServiceElement));
			Assertions.assertTrue(
				FreightDemandGenerationUtils.checkPositionInShape(link, null, indexShape, null, null));
			Assertions.assertFalse(FreightDemandGenerationUtils.checkPositionInShape(link, null, indexShape,
				new String[] { "area1" }, null));
			Assertions.assertTrue(FreightDemandGenerationUtils.checkPositionInShape(link, null, indexShape,
				new String[] { "area2" }, null));
		}
		Assertions.assertEquals(4, locationsPerServiceElement.get("serviceElement2").size());
		Assertions.assertTrue(locationsPerServiceElement.get("serviceElement2").contains("i(2,0)"));

		// check carrier 2
		Carrier testCarrier2 = CarriersUtils.getCarriers(scenario).getCarriers()
			.get(Id.create("testCarrier2", Carrier.class));
		Assertions.assertEquals(0, testCarrier2.getServices().size());
		Assertions.assertEquals(11, testCarrier2.getShipments().size());
		Object2IntMap<Integer> countShipmentsWithCertainDemand = new Object2IntOpenHashMap<>();
		Map<String, Set<String>> locationsPerShipmentElement = new HashMap<>();
		countDemand = 0;
		for (CarrierShipment shipment : testCarrier2.getShipments().values()) {
            countShipmentsWithCertainDemand.merge((Integer) shipment.getCapacityDemand(), 1, Integer::sum);
            countDemand = countDemand + shipment.getCapacityDemand();
            if (shipment.getCapacityDemand() == 0) {
				Assertions.assertEquals(300, shipment.getPickupDuration(), MatsimTestUtils.EPSILON);
				Assertions.assertEquals(350, shipment.getDeliveryDuration(), MatsimTestUtils.EPSILON);
				Assertions.assertEquals(TimeWindow.newInstance(10000, 45000), shipment.getPickupStartingTimeWindow());
				Assertions.assertEquals(TimeWindow.newInstance(11000, 44000), shipment.getDeliveryStartingTimeWindow());
				locationsPerShipmentElement.computeIfAbsent("ShipmentElement1_pickup", (k) -> new HashSet<>())
					.add(shipment.getPickupLinkId().toString());
				locationsPerShipmentElement.computeIfAbsent("ShipmentElement1_delivery", (k) -> new HashSet<>())
					.add(shipment.getDeliveryLinkId().toString());
			} else if (shipment.getCapacityDemand() == 2) {
				Assertions.assertEquals(400, shipment.getPickupDuration(), MatsimTestUtils.EPSILON);
				Assertions.assertEquals(400, shipment.getDeliveryDuration(), MatsimTestUtils.EPSILON);
				Assertions.assertEquals(TimeWindow.newInstance(11000, 44000), shipment.getPickupStartingTimeWindow());
				Assertions.assertEquals(TimeWindow.newInstance(20000, 40000), shipment.getDeliveryStartingTimeWindow());
				locationsPerShipmentElement.computeIfAbsent("ShipmentElement2_pickup", (k) -> new HashSet<>())
					.add(shipment.getPickupLinkId().toString());
				locationsPerShipmentElement.computeIfAbsent("ShipmentElement2_delivery", (k) -> new HashSet<>())
					.add(shipment.getDeliveryLinkId().toString());
			} else {
                if (shipment.getCapacityDemand() == 3) {
                    Assertions.assertEquals(600, shipment.getPickupDuration(), MatsimTestUtils.EPSILON);
                    Assertions.assertEquals(600, shipment.getDeliveryDuration(), MatsimTestUtils.EPSILON);
					Assertions.assertEquals(TimeWindow.newInstance(11000, 44000), shipment.getPickupStartingTimeWindow());
					Assertions.assertEquals(TimeWindow.newInstance(20000, 40000), shipment.getDeliveryStartingTimeWindow());
                    locationsPerShipmentElement.computeIfAbsent("ShipmentElement2_pickup", (k) -> new HashSet<>())
                            .add(shipment.getPickupLinkId().toString());
                    locationsPerShipmentElement.computeIfAbsent("ShipmentElement2_delivery", (k) -> new HashSet<>())
                            .add(shipment.getDeliveryLinkId().toString());
                } else
                    Assertions.fail("Shipment has an unexpected demand.");
            }
		}
		Assertions.assertEquals(15, countDemand);
		Assertions.assertEquals(4, countShipmentsWithCertainDemand.getInt(0));
		Assertions.assertEquals(6, countShipmentsWithCertainDemand.getInt(2));
		Assertions.assertEquals(1, countShipmentsWithCertainDemand.getInt(3));
		Assertions.assertEquals(4, locationsPerShipmentElement.get("ShipmentElement1_pickup").size());
		Assertions.assertEquals(1, locationsPerShipmentElement.get("ShipmentElement1_delivery").size());
		Assertions.assertTrue(locationsPerShipmentElement.get("ShipmentElement1_delivery").contains("i(2,0)"));
		Assertions.assertEquals(1, locationsPerShipmentElement.get("ShipmentElement2_pickup").size());
		Assertions.assertEquals(2, locationsPerShipmentElement.get("ShipmentElement2_delivery").size());
	}

	/**
	 * Results after combing jobs.
	 *
	 * @param scenario   the scenario
	 * @param network    the network
	 * @param indexShape the index of the shape
	 */
	private static void checkCarrier1and2WithCombiningJobs(Scenario scenario, Network network, ShpOptions.Index indexShape) {
		Carrier testCarrier1 = CarriersUtils.getCarriers(scenario).getCarriers()
			.get(Id.create("testCarrier1", Carrier.class));
		Assertions.assertEquals(8, testCarrier1.getServices().size());
		Assertions.assertEquals(0, testCarrier1.getShipments().size());
		Object2IntMap<Integer> countServicesWithCertainDemand = new Object2IntOpenHashMap<>();
		Map<String, Set<String>> locationsPerServiceElement = new HashMap<>();
		int countDemand = 0;
		for (CarrierService service : testCarrier1.getServices().values()) {
			countServicesWithCertainDemand.merge((Integer) service.getCapacityDemand(), 1, Integer::sum);
			countDemand = countDemand + service.getCapacityDemand();
			if (service.getCapacityDemand() == 0) {
				Assertions.assertEquals(180, service.getServiceDuration(), MatsimTestUtils.EPSILON);
				Assertions.assertEquals(TimeWindow.newInstance(3000, 13000), service.getServiceStaringTimeWindow());
				locationsPerServiceElement.computeIfAbsent("serviceElement1", (k) -> new HashSet<>())
					.add(service.getServiceLinkId().toString());
			} else {
				Assertions.assertEquals(service.getCapacityDemand() * 100, service.getServiceDuration(), MatsimTestUtils.EPSILON);
				Assertions.assertEquals(TimeWindow.newInstance(5000, 20000), service.getServiceStaringTimeWindow());
				locationsPerServiceElement.computeIfAbsent("serviceElement2", (k) -> new HashSet<>())
					.add(service.getServiceLinkId().toString());
			}
		}
		Assertions.assertEquals(12, countDemand);
		Assertions.assertEquals(4, countServicesWithCertainDemand.getInt(0));
		Assertions.assertEquals(4, locationsPerServiceElement.get("serviceElement1").size());
		for (String locationsOfServiceElement : locationsPerServiceElement.get("serviceElement1")) {
			Link link = network.getLinks().get(Id.createLinkId(locationsOfServiceElement));
			Assertions.assertTrue(
				FreightDemandGenerationUtils.checkPositionInShape(link, null, indexShape, null, null));
			Assertions.assertFalse(FreightDemandGenerationUtils.checkPositionInShape(link, null, indexShape,
				new String[] { "area1" }, null));
			Assertions.assertTrue(FreightDemandGenerationUtils.checkPositionInShape(link, null, indexShape,
				new String[] { "area2" }, null));
		}
		Assertions.assertEquals(4, locationsPerServiceElement.get("serviceElement2").size());
		Assertions.assertTrue(locationsPerServiceElement.get("serviceElement2").contains("i(2,0)"));

		// check carrier 2
		Carrier testCarrier2 = CarriersUtils.getCarriers(scenario).getCarriers()
			.get(Id.create("testCarrier2", Carrier.class));
		Assertions.assertEquals(0, testCarrier2.getServices().size());
		Assertions.assertEquals(6, testCarrier2.getShipments().size());
		Object2IntMap<Integer> countShipmentsWithCertainDemand = new Object2IntOpenHashMap<>();
		Map<String, Set<String>> locationsPerShipmentElement = new HashMap<>();
		countDemand = 0;
		for (CarrierShipment shipment : testCarrier2.getShipments().values()) {
            countShipmentsWithCertainDemand.merge((Integer) shipment.getCapacityDemand(), 1, Integer::sum);
            countDemand = countDemand + shipment.getCapacityDemand();
            if (shipment.getCapacityDemand() == 0) {
				Assertions.assertEquals(300, shipment.getPickupDuration(), MatsimTestUtils.EPSILON);
				Assertions.assertEquals(350, shipment.getDeliveryDuration(), MatsimTestUtils.EPSILON);
				Assertions.assertEquals(TimeWindow.newInstance(10000, 45000), shipment.getPickupStartingTimeWindow());
				Assertions.assertEquals(TimeWindow.newInstance(11000, 44000), shipment.getDeliveryStartingTimeWindow());
				locationsPerShipmentElement.computeIfAbsent("ShipmentElement1_pickup", (k) -> new HashSet<>())
					.add(shipment.getPickupLinkId().toString());
				locationsPerShipmentElement.computeIfAbsent("ShipmentElement1_delivery", (k) -> new HashSet<>())
					.add(shipment.getDeliveryLinkId().toString());
			} else {
                Assertions.assertEquals(shipment.getCapacityDemand() * 200, shipment.getPickupDuration(), MatsimTestUtils.EPSILON);
                Assertions.assertEquals(shipment.getCapacityDemand() * 200, shipment.getDeliveryDuration(), MatsimTestUtils.EPSILON);
				Assertions.assertEquals(TimeWindow.newInstance(11000, 44000), shipment.getPickupStartingTimeWindow());
				Assertions.assertEquals(TimeWindow.newInstance(20000, 40000), shipment.getDeliveryStartingTimeWindow());
				locationsPerShipmentElement.computeIfAbsent("ShipmentElement2_pickup", (k) -> new HashSet<>())
					.add(shipment.getPickupLinkId().toString());
				locationsPerShipmentElement.computeIfAbsent("ShipmentElement2_delivery", (k) -> new HashSet<>())
					.add(shipment.getDeliveryLinkId().toString());
			}
		}
		Assertions.assertEquals(15, countDemand);
		Assertions.assertEquals(4, countShipmentsWithCertainDemand.getInt(0));
		Assertions.assertEquals(4, locationsPerShipmentElement.get("ShipmentElement1_pickup").size());
		Assertions.assertEquals(1, locationsPerShipmentElement.get("ShipmentElement1_delivery").size());
		Assertions.assertTrue(locationsPerShipmentElement.get("ShipmentElement1_delivery").contains("i(2,0)"));
		Assertions.assertEquals(1, locationsPerShipmentElement.get("ShipmentElement2_pickup").size());
		Assertions.assertEquals(2, locationsPerShipmentElement.get("ShipmentElement2_delivery").size());
	}
}
