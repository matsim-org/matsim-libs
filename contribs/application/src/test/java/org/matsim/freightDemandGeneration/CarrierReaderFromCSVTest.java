package org.matsim.freightDemandGeneration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.options.ShpOptions;
import org.matsim.contrib.freight.FreightConfigGroup;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.contrib.freight.carrier.CarrierUtils;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
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

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void carrierCreation() throws IOException {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(
				"https://raw.githubusercontent.com/matsim-org/matsim/master/examples/scenarios/freight-chessboard-9x9/grid9x9.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		FreightConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule(scenario.getConfig(),
				FreightConfigGroup.class);
		freightConfigGroup.setCarriersVehicleTypesFile(utils.getPackageInputDirectory() + "testVehicleTypes.xml");
		String carrierCSVLocation = utils.getPackageInputDirectory() + "testCarrierCSV.csv";
		Path shapeFilePath = Path.of(utils.getPackageInputDirectory() + "testShape/testShape.shp");
		ShpOptions shp = new ShpOptions(shapeFilePath, "WGS84", null);
		Collection<SimpleFeature> polygonsInShape = shp.readFeatures();
		Set<CarrierInformationElement> allNewCarrierInformation = CarrierReaderFromCSV
				.readCarrierInformation(carrierCSVLocation);
		CarrierReaderFromCSV.checkNewCarrier(allNewCarrierInformation, freightConfigGroup, scenario, polygonsInShape);
		CarrierReaderFromCSV.createNewCarrierAndAddVehilceTypes(scenario, allNewCarrierInformation, freightConfigGroup,
				polygonsInShape, 1, null);
		Assert.assertEquals(3, FreightUtils.getCarriers(scenario).getCarriers().size());
		Assert.assertTrue(
				FreightUtils.getCarriers(scenario).getCarriers().containsKey(Id.create("testCarrier1", Carrier.class)));
		Assert.assertTrue(
				FreightUtils.getCarriers(scenario).getCarriers().containsKey(Id.create("testCarrier2", Carrier.class)));
		Assert.assertTrue(
				FreightUtils.getCarriers(scenario).getCarriers().containsKey(Id.create("testCarrier3", Carrier.class)));

		// check carrier 1
		Carrier testCarrier1 = FreightUtils.getCarriers(scenario).getCarriers()
				.get(Id.create("testCarrier1", Carrier.class));
		Assert.assertEquals(FleetSize.INFINITE, testCarrier1.getCarrierCapabilities().getFleetSize());
		Assert.assertEquals(10, CarrierUtils.getJspritIterations(testCarrier1));
		Assert.assertEquals(4, testCarrier1.getCarrierCapabilities().getCarrierVehicles().size());
		Object2IntMap<String> depotSums = new Object2IntOpenHashMap<>();
		Map<String, List<String>> typesPerDepot = new HashMap<>();
		for (CarrierVehicle carrierVehicle : testCarrier1.getCarrierCapabilities().getCarrierVehicles().values()) {
			typesPerDepot.computeIfAbsent(carrierVehicle.getLocation().toString(), (k) -> new ArrayList<>())
					.add(carrierVehicle.getVehicleTypeId().toString());
			depotSums.merge(carrierVehicle.getLocation().toString(), 1, Integer::sum);
			Assert.assertEquals(3600, carrierVehicle.getEarliestStartTime(), MatsimTestUtils.EPSILON);
			Assert.assertEquals(50000, carrierVehicle.getLatestEndTime(), MatsimTestUtils.EPSILON);
		}
		Assert.assertEquals(2, depotSums.size());
		Assert.assertTrue(depotSums.containsKey("i(2,0)"));
		Assert.assertEquals(2, depotSums.getInt("i(2,0)"));
		Assert.assertTrue(depotSums.containsKey("j(2,4)R"));
		Assert.assertEquals(2, depotSums.getInt("j(2,4)R"));
		Assert.assertEquals(2, typesPerDepot.size());
		Assert.assertTrue(typesPerDepot.containsKey("i(2,0)"));
		Assert.assertEquals(2, typesPerDepot.get("i(2,0)").size());
		Assert.assertTrue(typesPerDepot.get("i(2,0)").contains("testVehicle1"));
		Assert.assertTrue(typesPerDepot.get("i(2,0)").contains("testVehicle2"));
		Assert.assertTrue(typesPerDepot.containsKey("j(2,4)R"));
		Assert.assertEquals(2, typesPerDepot.get("j(2,4)R").size());
		Assert.assertTrue(typesPerDepot.get("j(2,4)R").contains("testVehicle1"));
		Assert.assertTrue(typesPerDepot.get("j(2,4)R").contains("testVehicle2"));

		// check carrier 2
		Carrier testCarrier2 = FreightUtils.getCarriers(scenario).getCarriers()
				.get(Id.create("testCarrier2", Carrier.class));
		Assert.assertEquals(FleetSize.FINITE, testCarrier2.getCarrierCapabilities().getFleetSize());
		Assert.assertEquals(15, CarrierUtils.getJspritIterations(testCarrier2));
		Assert.assertEquals(9, testCarrier2.getCarrierCapabilities().getCarrierVehicles().size());
		depotSums = new Object2IntOpenHashMap<>();
		typesPerDepot = new HashMap<>();
		for (CarrierVehicle carrierVehicle : testCarrier2.getCarrierCapabilities().getCarrierVehicles().values()) {
			typesPerDepot.computeIfAbsent(carrierVehicle.getLocation().toString(), (k) -> new ArrayList<>())
					.add(carrierVehicle.getVehicleTypeId().toString());
			depotSums.merge(carrierVehicle.getLocation().toString(), 1, Integer::sum);
			Assert.assertEquals(3600, carrierVehicle.getEarliestStartTime(), MatsimTestUtils.EPSILON);
			Assert.assertEquals(50000, carrierVehicle.getLatestEndTime(), MatsimTestUtils.EPSILON);
		}
		Assert.assertEquals(3, depotSums.size());
		Assert.assertTrue(depotSums.containsKey("j(4,3)R"));
		Assert.assertEquals(3, depotSums.getInt("j(4,3)R"));
		Assert.assertEquals(3, typesPerDepot.size());
		Assert.assertTrue(typesPerDepot.containsKey("j(4,3)R"));
		Assert.assertEquals(3, typesPerDepot.get("j(4,3)R").size());
		Assert.assertTrue(typesPerDepot.get("j(4,3)R").contains("testVehicle2"));

		// check carrier 3
		Network network = NetworkUtils.readNetwork(
				"https://raw.githubusercontent.com/matsim-org/matsim-libs/master/examples/scenarios/freight-chessboard-9x9/grid9x9.xml");
		Carrier testCarrier3 = FreightUtils.getCarriers(scenario).getCarriers()
				.get(Id.create("testCarrier3", Carrier.class));
		Assert.assertEquals(FleetSize.INFINITE, testCarrier3.getCarrierCapabilities().getFleetSize());
		Assert.assertEquals(1, CarrierUtils.getJspritIterations(testCarrier3));
		Assert.assertEquals(2, testCarrier3.getCarrierCapabilities().getCarrierVehicles().size());
		depotSums = new Object2IntOpenHashMap<>();
		typesPerDepot = new HashMap<>();
		for (CarrierVehicle carrierVehicle : testCarrier3.getCarrierCapabilities().getCarrierVehicles().values()) {
			typesPerDepot.computeIfAbsent(carrierVehicle.getLocation().toString(), (k) -> new ArrayList<>())
					.add(carrierVehicle.getVehicleTypeId().toString());
			depotSums.merge(carrierVehicle.getLocation().toString(), 1, Integer::sum);
			Assert.assertEquals(50000, carrierVehicle.getEarliestStartTime(), MatsimTestUtils.EPSILON);
			Assert.assertEquals(80000, carrierVehicle.getLatestEndTime(), MatsimTestUtils.EPSILON);
		}
		Assert.assertEquals(2, depotSums.size());
		Assert.assertTrue(depotSums.containsKey("j(2,6)R"));
		Assert.assertEquals(1, depotSums.getInt("j(2,6)R"));
		for (String depot : depotSums.keySet()) {
			if (!depot.equals("j(2,6)R")) {
				Link link = network.getLinks().get(Id.createLinkId(depot));
				Assert.assertTrue(
						FreightDemandGenerationUtils.checkPositionInShape(link, null, polygonsInShape, null, null));
				Assert.assertTrue(FreightDemandGenerationUtils.checkPositionInShape(link, null, polygonsInShape, new String[]{"area1"}, null));
				Assert.assertFalse(FreightDemandGenerationUtils.checkPositionInShape(link, null, polygonsInShape, new String[]{"area2"}, null));
			}

		}
		Assert.assertEquals(2, typesPerDepot.size());
		Assert.assertTrue(typesPerDepot.containsKey("j(2,6)R"));
		Assert.assertEquals(1, typesPerDepot.get("j(2,6)R").size());
		Assert.assertTrue(typesPerDepot.get("j(2,6)R").contains("testVehicle1"));

	}

	@Test
	public void csvCarrierReader() throws IOException {

		String carrierCSVLocation = utils.getPackageInputDirectory() + "testCarrierCSV.csv";
		Set<CarrierInformationElement> allNewCarrierInformation = CarrierReaderFromCSV
				.readCarrierInformation(carrierCSVLocation);
		Assert.assertEquals(3, allNewCarrierInformation.size());

		for (CarrierInformationElement carrierInformationElement : allNewCarrierInformation) {
			if (carrierInformationElement.getName().equals("testCarrier1")) {
				Assert.assertNull(carrierInformationElement.getAreaOfAdditonalDepots());
				Assert.assertEquals(0, carrierInformationElement.getFixedNumberOfVehilcePerTypeAndLocation());
				Assert.assertEquals(FleetSize.INFINITE, carrierInformationElement.getFleetSize());
				Assert.assertEquals(10, carrierInformationElement.getJspritIterations());
				Assert.assertEquals(2, carrierInformationElement.getNumberOfDepotsPerType());
				Assert.assertEquals(3600, carrierInformationElement.getVehicleStartTime());
				Assert.assertEquals(50000, carrierInformationElement.getVehicleEndTime());
				Assert.assertEquals(2, carrierInformationElement.getVehicleDepots().length);
				Assert.assertTrue(carrierInformationElement.getVehicleDepots()[0].equals("i(2,0)")
						|| carrierInformationElement.getVehicleDepots()[0].equals("j(2,4)R"));
				Assert.assertFalse(carrierInformationElement.getVehicleDepots()[0]
						.equals(carrierInformationElement.getVehicleDepots()[1]));
				Assert.assertEquals(2, carrierInformationElement.getVehicleTypes().length);
				Assert.assertTrue(carrierInformationElement.getVehicleTypes()[0].equals("testVehicle1")
						|| carrierInformationElement.getVehicleTypes()[0].equals("testVehicle2"));
				Assert.assertFalse(carrierInformationElement.getVehicleTypes()[0]
						.equals(carrierInformationElement.getVehicleTypes()[1]));

			} else if (carrierInformationElement.getName().equals("testCarrier3")) {
				Assert.assertEquals(1, carrierInformationElement.getAreaOfAdditonalDepots().length);
				Assert.assertEquals("area1", carrierInformationElement.getAreaOfAdditonalDepots()[0]);
				Assert.assertEquals(0, carrierInformationElement.getFixedNumberOfVehilcePerTypeAndLocation());
				Assert.assertEquals(FleetSize.INFINITE, carrierInformationElement.getFleetSize());
				Assert.assertEquals(0, carrierInformationElement.getJspritIterations());
				Assert.assertEquals(2, carrierInformationElement.getNumberOfDepotsPerType());
				Assert.assertEquals(50000, carrierInformationElement.getVehicleStartTime());
				Assert.assertEquals(80000, carrierInformationElement.getVehicleEndTime());
				Assert.assertEquals(1, carrierInformationElement.getVehicleDepots().length);
				Assert.assertEquals("j(2,6)R", carrierInformationElement.getVehicleDepots()[0]);
				Assert.assertEquals(1, carrierInformationElement.getVehicleTypes().length);
				Assert.assertEquals("testVehicle1", carrierInformationElement.getVehicleTypes()[0]);

			} else if (carrierInformationElement.getName().equals("testCarrier2")) {
				Assert.assertNull(carrierInformationElement.getAreaOfAdditonalDepots());
				Assert.assertEquals(3, carrierInformationElement.getFixedNumberOfVehilcePerTypeAndLocation());
				Assert.assertEquals(FleetSize.FINITE, carrierInformationElement.getFleetSize());
				Assert.assertEquals(15, carrierInformationElement.getJspritIterations());
				Assert.assertEquals(3, carrierInformationElement.getNumberOfDepotsPerType());
				Assert.assertEquals(3600, carrierInformationElement.getVehicleStartTime());
				Assert.assertEquals(50000, carrierInformationElement.getVehicleEndTime());
				Assert.assertEquals(1, carrierInformationElement.getVehicleDepots().length);
				Assert.assertEquals("j(4,3)R", carrierInformationElement.getVehicleDepots()[0]);
				Assert.assertEquals(1, carrierInformationElement.getVehicleTypes().length);
				Assert.assertEquals("testVehicle2", carrierInformationElement.getVehicleTypes()[0]);
			} else {
				Assert.fail("No expected carrierInformationElement found");
			}
		}

	}
}
