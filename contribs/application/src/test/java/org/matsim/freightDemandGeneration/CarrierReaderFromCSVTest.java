package org.matsim.freightDemandGeneration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.options.ShpOptions;
import org.matsim.freight.carriers.FreightCarriersConfigGroup;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarrierCapabilities.FleetSize;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.freight.carriers.CarrierVehicle;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.freightDemandGeneration.CarrierReaderFromCSV.CarrierInformationElement;
import org.matsim.testcases.MatsimTestUtils;
import org.opengis.feature.simple.SimpleFeature;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/**
 * @author Ricardo Ewert
 *
 */
public class CarrierReaderFromCSVTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void carrierCreation() throws IOException {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(
				"https://raw.githubusercontent.com/matsim-org/matsim/master/examples/scenarios/freight-chessboard-9x9/grid9x9.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		FreightCarriersConfigGroup freightCarriersConfigGroup = ConfigUtils.addOrGetModule(scenario.getConfig(),
				FreightCarriersConfigGroup.class);
		freightCarriersConfigGroup.setCarriersVehicleTypesFile(utils.getPackageInputDirectory() + "testVehicleTypes.xml");
		Path carrierCSVLocation = Path.of(utils.getPackageInputDirectory() + "testCarrierCSV.csv");
		Path shapeFilePath = Path.of(utils.getPackageInputDirectory() + "testShape/testShape.shp");
		ShpOptions shp = new ShpOptions(shapeFilePath, "WGS84", null);
		Collection<SimpleFeature> polygonsInShape = shp.readFeatures();
		Set<CarrierInformationElement> allNewCarrierInformation = CarrierReaderFromCSV
				.readCarrierInformation(carrierCSVLocation);
		String shapeCategory = "Ortsteil";
		CarrierReaderFromCSV.checkNewCarrier(allNewCarrierInformation, freightCarriersConfigGroup, scenario, polygonsInShape, shapeCategory);
		CarrierReaderFromCSV.createNewCarrierAndAddVehicleTypes(scenario, allNewCarrierInformation, freightCarriersConfigGroup,
				polygonsInShape, 1, null);
		Assertions.assertEquals(3, CarriersUtils.getCarriers(scenario).getCarriers().size());
		Assertions.assertTrue(
				CarriersUtils.getCarriers(scenario).getCarriers().containsKey(Id.create("testCarrier1", Carrier.class)));
		Assertions.assertTrue(
				CarriersUtils.getCarriers(scenario).getCarriers().containsKey(Id.create("testCarrier2", Carrier.class)));
		Assertions.assertTrue(
				CarriersUtils.getCarriers(scenario).getCarriers().containsKey(Id.create("testCarrier3", Carrier.class)));

		// check carrier 1
		Carrier testCarrier1 = CarriersUtils.getCarriers(scenario).getCarriers()
				.get(Id.create("testCarrier1", Carrier.class));
		Assertions.assertEquals(FleetSize.INFINITE, testCarrier1.getCarrierCapabilities().getFleetSize());
		Assertions.assertEquals(10, CarriersUtils.getJspritIterations(testCarrier1));
		Assertions.assertEquals(4, testCarrier1.getCarrierCapabilities().getCarrierVehicles().size());
		Object2IntMap<String> depotSums = new Object2IntOpenHashMap<>();
		Map<String, List<String>> typesPerDepot = new HashMap<>();
		for (CarrierVehicle carrierVehicle : testCarrier1.getCarrierCapabilities().getCarrierVehicles().values()) {
			typesPerDepot.computeIfAbsent(carrierVehicle.getLinkId().toString(), ( k) -> new ArrayList<>() )
					.add(carrierVehicle.getVehicleTypeId().toString());
			depotSums.merge(carrierVehicle.getLinkId().toString(), 1, Integer::sum );
			Assertions.assertEquals(3600, carrierVehicle.getEarliestStartTime(), MatsimTestUtils.EPSILON);
			Assertions.assertEquals(50000, carrierVehicle.getLatestEndTime(), MatsimTestUtils.EPSILON);
		}
		Assertions.assertEquals(2, depotSums.size());
		Assertions.assertTrue(depotSums.containsKey("i(2,0)"));
		Assertions.assertEquals(2, depotSums.getInt("i(2,0)"));
		Assertions.assertTrue(depotSums.containsKey("j(2,4)R"));
		Assertions.assertEquals(2, depotSums.getInt("j(2,4)R"));
		Assertions.assertEquals(2, typesPerDepot.size());
		Assertions.assertTrue(typesPerDepot.containsKey("i(2,0)"));
		Assertions.assertEquals(2, typesPerDepot.get("i(2,0)").size());
		Assertions.assertTrue(typesPerDepot.get("i(2,0)").contains("testVehicle1"));
		Assertions.assertTrue(typesPerDepot.get("i(2,0)").contains("testVehicle2"));
		Assertions.assertTrue(typesPerDepot.containsKey("j(2,4)R"));
		Assertions.assertEquals(2, typesPerDepot.get("j(2,4)R").size());
		Assertions.assertTrue(typesPerDepot.get("j(2,4)R").contains("testVehicle1"));
		Assertions.assertTrue(typesPerDepot.get("j(2,4)R").contains("testVehicle2"));

		// check carrier 2
		Carrier testCarrier2 = CarriersUtils.getCarriers(scenario).getCarriers()
				.get(Id.create("testCarrier2", Carrier.class));
		Assertions.assertEquals(FleetSize.FINITE, testCarrier2.getCarrierCapabilities().getFleetSize());
		Assertions.assertEquals(15, CarriersUtils.getJspritIterations(testCarrier2));
		Assertions.assertEquals(9, testCarrier2.getCarrierCapabilities().getCarrierVehicles().size());
		depotSums = new Object2IntOpenHashMap<>();
		typesPerDepot = new HashMap<>();
		for (CarrierVehicle carrierVehicle : testCarrier2.getCarrierCapabilities().getCarrierVehicles().values()) {
			typesPerDepot.computeIfAbsent(carrierVehicle.getLinkId().toString(), ( k) -> new ArrayList<>() )
					.add(carrierVehicle.getVehicleTypeId().toString());
			depotSums.merge(carrierVehicle.getLinkId().toString(), 1, Integer::sum );
			Assertions.assertEquals(3600, carrierVehicle.getEarliestStartTime(), MatsimTestUtils.EPSILON);
			Assertions.assertEquals(50000, carrierVehicle.getLatestEndTime(), MatsimTestUtils.EPSILON);
		}
		Assertions.assertEquals(3, depotSums.size());
		Assertions.assertTrue(depotSums.containsKey("j(4,3)R"));
		Assertions.assertEquals(3, depotSums.getInt("j(4,3)R"));
		Assertions.assertEquals(3, typesPerDepot.size());
		Assertions.assertTrue(typesPerDepot.containsKey("j(4,3)R"));
		Assertions.assertEquals(3, typesPerDepot.get("j(4,3)R").size());
		Assertions.assertTrue(typesPerDepot.get("j(4,3)R").contains("testVehicle2"));

		// check carrier 3
		Network network = NetworkUtils.readNetwork(
				"https://raw.githubusercontent.com/matsim-org/matsim-libs/master/examples/scenarios/freight-chessboard-9x9/grid9x9.xml");
		Carrier testCarrier3 = CarriersUtils.getCarriers(scenario).getCarriers()
				.get(Id.create("testCarrier3", Carrier.class));
		Assertions.assertEquals(FleetSize.INFINITE, testCarrier3.getCarrierCapabilities().getFleetSize());
		Assertions.assertEquals(1, CarriersUtils.getJspritIterations(testCarrier3));
		Assertions.assertEquals(2, testCarrier3.getCarrierCapabilities().getCarrierVehicles().size());
		depotSums = new Object2IntOpenHashMap<>();
		typesPerDepot = new HashMap<>();
		for (CarrierVehicle carrierVehicle : testCarrier3.getCarrierCapabilities().getCarrierVehicles().values()) {
			typesPerDepot.computeIfAbsent(carrierVehicle.getLinkId().toString(), ( k) -> new ArrayList<>() )
					.add(carrierVehicle.getVehicleTypeId().toString());
			depotSums.merge(carrierVehicle.getLinkId().toString(), 1, Integer::sum );
			Assertions.assertEquals(50000, carrierVehicle.getEarliestStartTime(), MatsimTestUtils.EPSILON);
			Assertions.assertEquals(80000, carrierVehicle.getLatestEndTime(), MatsimTestUtils.EPSILON);
		}
		Assertions.assertEquals(2, depotSums.size());
		Assertions.assertTrue(depotSums.containsKey("j(2,6)R"));
		Assertions.assertEquals(1, depotSums.getInt("j(2,6)R"));
		for (String depot : depotSums.keySet()) {
			if (!depot.equals("j(2,6)R")) {
				Link link = network.getLinks().get(Id.createLinkId(depot));
				Assertions.assertTrue(
						FreightDemandGenerationUtils.checkPositionInShape(link, null, polygonsInShape, null, null));
				Assertions.assertTrue(FreightDemandGenerationUtils.checkPositionInShape(link, null, polygonsInShape, new String[]{"area1"}, null));
				Assertions.assertFalse(FreightDemandGenerationUtils.checkPositionInShape(link, null, polygonsInShape, new String[]{"area2"}, null));
			}

		}
		Assertions.assertEquals(2, typesPerDepot.size());
		Assertions.assertTrue(typesPerDepot.containsKey("j(2,6)R"));
		Assertions.assertEquals(1, typesPerDepot.get("j(2,6)R").size());
		Assertions.assertTrue(typesPerDepot.get("j(2,6)R").contains("testVehicle1"));

	}

	@Test
	void csvCarrierReader() throws IOException {

		Path carrierCSVLocation = Path.of(utils.getPackageInputDirectory() + "testCarrierCSV.csv");
		Set<CarrierInformationElement> allNewCarrierInformation = CarrierReaderFromCSV
				.readCarrierInformation(carrierCSVLocation);
		Assertions.assertEquals(3, allNewCarrierInformation.size());

		for (CarrierInformationElement carrierInformationElement : allNewCarrierInformation) {
			if (carrierInformationElement.getName().equals("testCarrier1")) {
				Assertions.assertNull(carrierInformationElement.getAreaOfAdditionalDepots());
				Assertions.assertEquals(0, carrierInformationElement.getFixedNumberOfVehiclePerTypeAndLocation());
				Assertions.assertEquals(FleetSize.INFINITE, carrierInformationElement.getFleetSize());
				Assertions.assertEquals(10, carrierInformationElement.getJspritIterations());
				Assertions.assertEquals(2, carrierInformationElement.getNumberOfDepotsPerType());
				Assertions.assertEquals(3600, carrierInformationElement.getVehicleStartTime());
				Assertions.assertEquals(50000, carrierInformationElement.getVehicleEndTime());
				Assertions.assertEquals(2, carrierInformationElement.getVehicleDepots().size());
				Assertions.assertTrue(carrierInformationElement.getVehicleDepots().contains("i(2,0)")
						&& carrierInformationElement.getVehicleDepots().contains("j(2,4)R"));
				Assertions.assertEquals(2, carrierInformationElement.getVehicleTypes().length);
				Assertions.assertTrue(carrierInformationElement.getVehicleTypes()[0].equals("testVehicle1")
						|| carrierInformationElement.getVehicleTypes()[0].equals("testVehicle2"));
				Assertions.assertNotEquals(carrierInformationElement.getVehicleTypes()[0], carrierInformationElement.getVehicleTypes()[1]);

			} else if (carrierInformationElement.getName().equals("testCarrier3")) {
				Assertions.assertEquals(1, carrierInformationElement.getAreaOfAdditionalDepots().length);
				Assertions.assertEquals("area1", carrierInformationElement.getAreaOfAdditionalDepots()[0]);
				Assertions.assertEquals(0, carrierInformationElement.getFixedNumberOfVehiclePerTypeAndLocation());
				Assertions.assertEquals(FleetSize.INFINITE, carrierInformationElement.getFleetSize());
				Assertions.assertEquals(0, carrierInformationElement.getJspritIterations());
				Assertions.assertEquals(2, carrierInformationElement.getNumberOfDepotsPerType());
				Assertions.assertEquals(50000, carrierInformationElement.getVehicleStartTime());
				Assertions.assertEquals(80000, carrierInformationElement.getVehicleEndTime());
				Assertions.assertEquals(1, carrierInformationElement.getVehicleDepots().size());
				Assertions.assertEquals("j(2,6)R", carrierInformationElement.getVehicleDepots().get(0));
				Assertions.assertEquals(1, carrierInformationElement.getVehicleTypes().length);
				Assertions.assertEquals("testVehicle1", carrierInformationElement.getVehicleTypes()[0]);

			} else if (carrierInformationElement.getName().equals("testCarrier2")) {
				Assertions.assertNull(carrierInformationElement.getAreaOfAdditionalDepots());
				Assertions.assertEquals(3, carrierInformationElement.getFixedNumberOfVehiclePerTypeAndLocation());
				Assertions.assertEquals(FleetSize.FINITE, carrierInformationElement.getFleetSize());
				Assertions.assertEquals(15, carrierInformationElement.getJspritIterations());
				Assertions.assertEquals(3, carrierInformationElement.getNumberOfDepotsPerType());
				Assertions.assertEquals(3600, carrierInformationElement.getVehicleStartTime());
				Assertions.assertEquals(50000, carrierInformationElement.getVehicleEndTime());
				Assertions.assertEquals(1, carrierInformationElement.getVehicleDepots().size());
				Assertions.assertEquals("j(4,3)R", carrierInformationElement.getVehicleDepots().get(0));
				Assertions.assertEquals(1, carrierInformationElement.getVehicleTypes().length);
				Assertions.assertEquals("testVehicle2", carrierInformationElement.getVehicleTypes()[0]);
			} else {
				Assertions.fail("No expected carrierInformationElement found");
			}
		}

	}
}
