package org.matsim.freightDemandGeneration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.freightDemandGeneration.CarrierReaderFromCSV.CarrierInformationElement;
import org.matsim.freightDemandGeneration.DemandReaderFromCSV.DemandInformationElement;
import org.matsim.testcases.MatsimTestUtils;
import org.opengis.feature.simple.SimpleFeature;

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
	void testLinkForPerson() throws IOException {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(
				"https://raw.githubusercontent.com/matsim-org/matsim-libs/master/examples/scenarios/freight-chessboard-9x9/grid9x9.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		String populationLocation = utils.getPackageInputDirectory() + "testPopulation.xml";
		Population population = PopulationUtils.readPopulation(populationLocation);
		FreightDemandGenerationUtils.preparePopulation(population, 1.0, 1.0, "changeNumberOfLocationsWithDemand");
		HashMap<Id<Person>, HashMap<Double, String>> nearestLinkPerPerson = new HashMap<>();
		for (Person person :  population.getPersons().values()) {
			DemandReaderFromCSV.findLinksForPersons(scenario, nearestLinkPerPerson, person);
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
	void demandCreation() throws IOException {
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
		String shapeCategory = "Ortsteil";
		Path shapeFilePath = Path.of(utils.getPackageInputDirectory() + "testShape/testShape.shp");
		ShpOptions shp = new ShpOptions(shapeFilePath, "WGS84", null);
		Collection<SimpleFeature> polygonsInShape = shp.readFeatures();
		String populationLocation = utils.getPackageInputDirectory() + "testPopulation.xml";
		Population population = PopulationUtils.readPopulation(populationLocation);
		FreightDemandGenerationUtils.preparePopulation(population, 0.5, 1.0, "changeNumberOfLocationsWithDemand");

		// run methods
		Set<CarrierInformationElement> allNewCarrierInformation = CarrierReaderFromCSV
				.readCarrierInformation(carrierCSVLocation);
		CarrierReaderFromCSV.createNewCarrierAndAddVehicleTypes(scenario, allNewCarrierInformation, freightCarriersConfigGroup,
				polygonsInShape, 1, null);
		Set<DemandInformationElement> demandInformation = DemandReaderFromCSV.readDemandInformation(demandCSVLocation);
		DemandReaderFromCSV.checkNewDemand(scenario, demandInformation, polygonsInShape, shapeCategory);
		DemandReaderFromCSV.createDemandForCarriers(scenario, polygonsInShape, demandInformation, population, false,
				null);
		Assertions.assertEquals(3, CarriersUtils.getCarriers(scenario).getCarriers().size());
		Assertions.assertTrue(
				CarriersUtils.getCarriers(scenario).getCarriers().containsKey(Id.create("testCarrier1", Carrier.class)));
		Assertions.assertTrue(
				CarriersUtils.getCarriers(scenario).getCarriers().containsKey(Id.create("testCarrier2", Carrier.class)));
		Assertions.assertTrue(
				CarriersUtils.getCarriers(scenario).getCarriers().containsKey(Id.create("testCarrier3", Carrier.class)));

		// check carrier 1
		Network network = NetworkUtils.readNetwork(
				"https://raw.githubusercontent.com/matsim-org/matsim-libs/master/examples/scenarios/freight-chessboard-9x9/grid9x9.xml");
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
				Assertions.assertEquals(TimeWindow.newInstance(3000, 13000), service.getServiceStartTimeWindow());
				locationsPerServiceElement.computeIfAbsent("serviceElement1", (k) -> new HashSet<>())
						.add(service.getLocationLinkId().toString());
			} else if (service.getCapacityDemand() == 1) {
				Assertions.assertEquals(100, service.getServiceDuration(), MatsimTestUtils.EPSILON);
				Assertions.assertEquals(TimeWindow.newInstance(5000, 20000), service.getServiceStartTimeWindow());
				locationsPerServiceElement.computeIfAbsent("serviceElement2", (k) -> new HashSet<>())
						.add(service.getLocationLinkId().toString());
			} else if (service.getCapacityDemand() == 2) {
				Assertions.assertEquals(200, service.getServiceDuration(), MatsimTestUtils.EPSILON);
				Assertions.assertEquals(TimeWindow.newInstance(5000, 20000), service.getServiceStartTimeWindow());
				locationsPerServiceElement.computeIfAbsent("serviceElement2", (k) -> new HashSet<>())
						.add(service.getLocationLinkId().toString());
			} else
				Assertions.fail("Service has a wrong demand.");
		}
		Assertions.assertEquals(12, countDemand);
		Assertions.assertEquals(4, countServicesWithCertainDemand.getInt(0));
		Assertions.assertEquals(8, countServicesWithCertainDemand.getInt(1));
		Assertions.assertEquals(2, countServicesWithCertainDemand.getInt(2));
		Assertions.assertEquals(4, locationsPerServiceElement.get("serviceElement1").size());
		for (String locationsOfServiceElement : locationsPerServiceElement.get("serviceElement1")) {
			Link link = network.getLinks().get(Id.createLinkId(locationsOfServiceElement));
			Assertions.assertTrue(
					FreightDemandGenerationUtils.checkPositionInShape(link, null, polygonsInShape, null, null));
			Assertions.assertFalse(FreightDemandGenerationUtils.checkPositionInShape(link, null, polygonsInShape,
					new String[] { "area1" }, null));
			Assertions.assertTrue(FreightDemandGenerationUtils.checkPositionInShape(link, null, polygonsInShape,
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
			countShipmentsWithCertainDemand.merge((Integer) shipment.getSize(), 1, Integer::sum);
			countDemand = countDemand + shipment.getSize();
			if (shipment.getSize() == 0) {
				Assertions.assertEquals(300, shipment.getPickupServiceTime(), MatsimTestUtils.EPSILON);
				Assertions.assertEquals(350, shipment.getDeliveryServiceTime(), MatsimTestUtils.EPSILON);
				Assertions.assertEquals(TimeWindow.newInstance(10000, 45000), shipment.getPickupTimeWindow());
				Assertions.assertEquals(TimeWindow.newInstance(11000, 44000), shipment.getDeliveryTimeWindow());
				locationsPerShipmentElement.computeIfAbsent("ShipmenElement1_pickup", (k) -> new HashSet<>())
						.add(shipment.getFrom().toString());
				locationsPerShipmentElement.computeIfAbsent("ShipmenElement1_delivery", (k) -> new HashSet<>())
						.add(shipment.getTo().toString());
			} else if (shipment.getSize() == 2) {
				Assertions.assertEquals(400, shipment.getPickupServiceTime(), MatsimTestUtils.EPSILON);
				Assertions.assertEquals(400, shipment.getDeliveryServiceTime(), MatsimTestUtils.EPSILON);
				Assertions.assertEquals(TimeWindow.newInstance(11000, 44000), shipment.getPickupTimeWindow());
				Assertions.assertEquals(TimeWindow.newInstance(20000, 40000), shipment.getDeliveryTimeWindow());
				locationsPerShipmentElement.computeIfAbsent("ShipmenElement2_pickup", (k) -> new HashSet<>())
						.add(shipment.getFrom().toString());
				locationsPerShipmentElement.computeIfAbsent("ShipmenElement2_delivery", (k) -> new HashSet<>())
						.add(shipment.getTo().toString());
			} else if (shipment.getSize() == 3) {
				Assertions.assertEquals(600, shipment.getPickupServiceTime(), MatsimTestUtils.EPSILON);
				Assertions.assertEquals(600, shipment.getDeliveryServiceTime(), MatsimTestUtils.EPSILON);
				Assertions.assertEquals(TimeWindow.newInstance(11000, 44000), shipment.getPickupTimeWindow());
				Assertions.assertEquals(TimeWindow.newInstance(20000, 40000), shipment.getDeliveryTimeWindow());
				locationsPerShipmentElement.computeIfAbsent("ShipmenElement2_pickup", (k) -> new HashSet<>())
						.add(shipment.getFrom().toString());
				locationsPerShipmentElement.computeIfAbsent("ShipmenElement2_delivery", (k) -> new HashSet<>())
						.add(shipment.getTo().toString());
			} else
				Assertions.fail("Shipment has an unexpected demand.");
		}
		Assertions.assertEquals(15, countDemand);
		Assertions.assertEquals(4, countShipmentsWithCertainDemand.getInt(0));
		Assertions.assertEquals(6, countShipmentsWithCertainDemand.getInt(2));
		Assertions.assertEquals(1, countShipmentsWithCertainDemand.getInt(3));
		Assertions.assertEquals(4, locationsPerShipmentElement.get("ShipmenElement1_pickup").size());
		Assertions.assertEquals(1, locationsPerShipmentElement.get("ShipmenElement1_delivery").size());
		Assertions.assertTrue(locationsPerShipmentElement.get("ShipmenElement1_delivery").contains("i(2,0)"));
		Assertions.assertEquals(1, locationsPerShipmentElement.get("ShipmenElement2_pickup").size());
		Assertions.assertEquals(2, locationsPerShipmentElement.get("ShipmenElement2_delivery").size());

		// check carrier 3
		Carrier testCarrier3 = CarriersUtils.getCarriers(scenario).getCarriers()
				.get(Id.create("testCarrier3", Carrier.class));
		Assertions.assertEquals(0, testCarrier3.getServices().size());
		Assertions.assertEquals(4, testCarrier3.getShipments().size());
		countShipmentsWithCertainDemand = new Object2IntOpenHashMap<>();
		locationsPerShipmentElement = new HashMap<>();
		countDemand = 0;
		for (CarrierShipment shipment : testCarrier3.getShipments().values()) {
			countShipmentsWithCertainDemand.merge((Integer) shipment.getSize(), 1, Integer::sum);
			countDemand = countDemand + shipment.getSize();
			Assertions.assertEquals(5, shipment.getSize());
			Assertions.assertEquals(2000, shipment.getPickupServiceTime(), MatsimTestUtils.EPSILON);
			Assertions.assertEquals(1250, shipment.getDeliveryServiceTime(), MatsimTestUtils.EPSILON);
			Assertions.assertEquals(TimeWindow.newInstance(8000, 50000), shipment.getPickupTimeWindow());
			Assertions.assertEquals(TimeWindow.newInstance(10000, 60000), shipment.getDeliveryTimeWindow());
			locationsPerShipmentElement.computeIfAbsent("ShipmenElement1_pickup", (k) -> new HashSet<>())
					.add(shipment.getFrom().toString());
			locationsPerShipmentElement.computeIfAbsent("ShipmenElement1_delivery", (k) -> new HashSet<>())
					.add(shipment.getTo().toString());
		}
		Assertions.assertEquals(20, countDemand);
		Assertions.assertEquals(4, countShipmentsWithCertainDemand.getInt(5));
		Assertions.assertEquals(2, locationsPerShipmentElement.get("ShipmenElement1_pickup").size());
		Assertions.assertEquals(4, locationsPerShipmentElement.get("ShipmenElement1_delivery").size());
		for (String locationsOfShipmentElement : locationsPerShipmentElement.get("ShipmenElement1_delivery")) {
			Link link = network.getLinks().get(Id.createLinkId(locationsOfShipmentElement));
			Assertions.assertTrue(
					FreightDemandGenerationUtils.checkPositionInShape(link, null, polygonsInShape, null, null));
			Assertions.assertTrue(FreightDemandGenerationUtils.checkPositionInShape(link, null, polygonsInShape,
					new String[] { "area1" }, null));
			Assertions.assertFalse(FreightDemandGenerationUtils.checkPositionInShape(link, null, polygonsInShape,
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
				Assertions.assertTrue(demandInformationElement.getLocationsOfSecondJobElement()[0].equals("i(2,0)"));
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
				Assertions.assertEquals(0.125, (double) demandInformationElement.getShareOfPopulationWithFirstJobElement(),
						MatsimTestUtils.EPSILON);
				Assertions.assertNull(demandInformationElement.getAreasFirstJobElement());
				Assertions.assertNull(demandInformationElement.getNumberOfFirstJobElementLocations());
				Assertions.assertNull(demandInformationElement.getLocationsOfFirstJobElement());
				Assertions.assertEquals(400, (int) demandInformationElement.getFirstJobElementTimePerUnit());
				Assertions.assertEquals(TimeWindow.newInstance(8000, 50000),
						demandInformationElement.getFirstJobElementTimeWindow());
				Assertions.assertEquals(0.4, (double) demandInformationElement.getShareOfPopulationWithSecondJobElement(),
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
}
