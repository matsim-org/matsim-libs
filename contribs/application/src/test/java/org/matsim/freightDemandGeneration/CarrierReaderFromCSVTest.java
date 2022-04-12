package org.matsim.freightDemandGeneration;

import static org.junit.Assert.assertEquals;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.freight.FreightConfigGroup;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.contrib.freight.carrier.CarrierUtils;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.freightDemandGeneration.CarrierReaderFromCSV.CarrierInformationElement;
import org.matsim.testcases.MatsimTestUtils;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class CarrierReaderFromCSVTest {
	
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void carrierCreation() throws IOException {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile("https://raw.githubusercontent.com/matsim-org/matsim/master/examples/scenarios/freight-chessboard-9x9/grid9x9.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		FreightConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule(scenario.getConfig(),
				FreightConfigGroup.class);
		freightConfigGroup.setCarriersVehicleTypesFile(utils.getPackageInputDirectory() + "testVehicleTypes.xml");
		String carrierCSVLocation = utils.getPackageInputDirectory() + "testCarrierCSV.csv";
		Set<CarrierInformationElement> allNewCarrierInformation = CarrierReaderFromCSV.ReadCarrierInformation(carrierCSVLocation);
		CarrierReaderFromCSV.checkNewCarrier(allNewCarrierInformation, freightConfigGroup, scenario, null);
		CarrierReaderFromCSV.createNewCarrierAndAddVehilceTypes(scenario, allNewCarrierInformation, freightConfigGroup, null, 1, null);
		Assert.assertTrue(FreightUtils.getCarriers(scenario).getCarriers().size() == 3);
		Assert.assertTrue(FreightUtils.getCarriers(scenario).getCarriers().containsKey(Id.create("testCarrier1", Carrier.class)));
		Assert.assertTrue(FreightUtils.getCarriers(scenario).getCarriers().containsKey(Id.create("testCarrier2", Carrier.class)));
		Assert.assertTrue(FreightUtils.getCarriers(scenario).getCarriers().containsKey(Id.create("testCarrier3", Carrier.class)));
		
		//check carrier 1
		Carrier testCarrier1 = FreightUtils.getCarriers(scenario).getCarriers().get(Id.create("testCarrier1", Carrier.class));
		Assert.assertTrue(testCarrier1.getCarrierCapabilities().getFleetSize().equals(FleetSize.INFINITE));
		Assert.assertTrue(CarrierUtils.getJspritIterations(testCarrier1) == 10);
		Assert.assertTrue(testCarrier1.getCarrierCapabilities().getCarrierVehicles().size() == 4);
		Object2IntMap<String> depotSums = new Object2IntOpenHashMap<>();
		Map<String, List<String>> typesPerDepot = new HashMap<>();
		for (CarrierVehicle carrierVehicle : testCarrier1.getCarrierCapabilities().getCarrierVehicles().values()) {
			typesPerDepot.computeIfAbsent(carrierVehicle.getLocation().toString(), (k) -> new ArrayList<>()).add(carrierVehicle.getVehicleTypeId().toString());
			depotSums.merge(carrierVehicle.getLocation().toString(), 1, Integer::sum);
			Assert.assertTrue(carrierVehicle.getEarliestStartTime() == 3600);
			Assert.assertTrue(carrierVehicle.getLatestEndTime() == 50000);
		}
		Assert.assertEquals(depotSums.size(), 2);
		Assert.assertTrue(depotSums.containsKey("i(2,0)"));
		Assert.assertEquals(depotSums.getInt("i(2,0)"), 2);
		Assert.assertTrue(depotSums.containsKey("j(2,4)R"));
		Assert.assertEquals(depotSums.getInt("j(2,4)R"), 2);
		Assert.assertEquals(typesPerDepot.size(), 2);
		Assert.assertTrue(typesPerDepot.containsKey("i(2,0)"));
		Assert.assertEquals(typesPerDepot.get("i(2,0)").size(), 2);
		Assert.assertTrue(typesPerDepot.get("i(2,0)").contains("testVehicle1"));
		Assert.assertTrue(typesPerDepot.get("i(2,0)").contains("testVehicle2"));
		Assert.assertTrue(typesPerDepot.containsKey("j(2,4)R"));
		Assert.assertEquals(typesPerDepot.get("j(2,4)R").size(), 2);
		Assert.assertTrue(typesPerDepot.get("j(2,4)R").contains("testVehicle1"));
		Assert.assertTrue(typesPerDepot.get("j(2,4)R").contains("testVehicle2"));
		
		//check carrier 2
		Carrier testCarrier2 = FreightUtils.getCarriers(scenario).getCarriers().get(Id.create("testCarrier2", Carrier.class));
		Assert.assertTrue(testCarrier2.getCarrierCapabilities().getFleetSize().equals(FleetSize.FINITE));
		Assert.assertTrue(CarrierUtils.getJspritIterations(testCarrier2) == 15);
		Assert.assertTrue(testCarrier2.getCarrierCapabilities().getCarrierVehicles().size() == 9);
		depotSums = new Object2IntOpenHashMap<>();
		typesPerDepot = new HashMap<>();
		for (CarrierVehicle carrierVehicle : testCarrier2.getCarrierCapabilities().getCarrierVehicles().values()) {
			typesPerDepot.computeIfAbsent(carrierVehicle.getLocation().toString(), (k) -> new ArrayList<>()).add(carrierVehicle.getVehicleTypeId().toString());
			depotSums.merge(carrierVehicle.getLocation().toString(), 1, Integer::sum);
			Assert.assertTrue(carrierVehicle.getEarliestStartTime() == 3600);
			Assert.assertTrue(carrierVehicle.getLatestEndTime() == 50000);
		}
		Assert.assertTrue(depotSums.size() == 3);
		Assert.assertTrue(depotSums.containsKey("j(4,3)R"));
		Assert.assertTrue(depotSums.getInt("j(4,3)R") == 3);
		Assert.assertTrue(typesPerDepot.size() == 3);
		Assert.assertTrue(typesPerDepot.containsKey("j(4,3)R"));
		Assert.assertTrue(typesPerDepot.get("j(4,3)R").size() == 3);
		Assert.assertTrue(typesPerDepot.get("j(4,3)R").contains("testVehicle2"));
		
		//check carrier 3
		Carrier testCarrier3 = FreightUtils.getCarriers(scenario).getCarriers().get(Id.create("testCarrier3", Carrier.class));
		Assert.assertTrue(testCarrier3.getCarrierCapabilities().getFleetSize().equals(FleetSize.INFINITE));
		Assert.assertTrue(CarrierUtils.getJspritIterations(testCarrier3) == 1);
		Assert.assertTrue(testCarrier3.getCarrierCapabilities().getCarrierVehicles().size() == 2);
		depotSums = new Object2IntOpenHashMap<>();
		typesPerDepot = new HashMap<>();
		for (CarrierVehicle carrierVehicle : testCarrier3.getCarrierCapabilities().getCarrierVehicles().values()) {
			typesPerDepot.computeIfAbsent(carrierVehicle.getLocation().toString(), (k) -> new ArrayList<>()).add(carrierVehicle.getVehicleTypeId().toString());
			depotSums.merge(carrierVehicle.getLocation().toString(), 1, Integer::sum);
			Assert.assertTrue(carrierVehicle.getEarliestStartTime() == 50000);
			Assert.assertTrue(carrierVehicle.getLatestEndTime() == 80000);
		}
		Assert.assertTrue(depotSums.size() == 2);
		Assert.assertTrue(depotSums.containsKey("j(2,6)R"));
		Assert.assertTrue(depotSums.getInt("j(2,6)R") == 1);
		Assert.assertTrue(typesPerDepot.size() == 2);
		Assert.assertTrue(typesPerDepot.containsKey("j(2,6)R"));
		Assert.assertTrue(typesPerDepot.get("j(2,6)R").size() == 1);
		Assert.assertTrue(typesPerDepot.get("j(2,6)R").contains("testVehicle1"));
		
	}
	
	@Test
	public void csvReader() throws IOException {
		
		String carrierCSVLocation = utils.getPackageInputDirectory() + "testCarrierCSV.csv";
		Set<CarrierInformationElement> allNewCarrierInformation = CarrierReaderFromCSV
				.ReadCarrierInformation(carrierCSVLocation);
		assertEquals(3, allNewCarrierInformation.size());
		
		for (CarrierInformationElement carrierInformationElement : allNewCarrierInformation) {
			if (carrierInformationElement.getName().equals("testCarrier1")) {
				Assert.assertTrue(carrierInformationElement.getAreaOfAdditonalDepots() == null);
				Assert.assertTrue(carrierInformationElement.getFixedNumberOfVehilcePerTypeAndLocation() == 0);
				Assert.assertTrue(carrierInformationElement.getFleetSize() == FleetSize.INFINITE);
				Assert.assertTrue(carrierInformationElement.getJspritIterations() == 10);
				Assert.assertTrue(carrierInformationElement.getNumberOfDepotsPerType() == 2);
				Assert.assertTrue(carrierInformationElement.getVehicleStartTime() == 3600);
				Assert.assertTrue(carrierInformationElement.getVehicleEndTime() == 50000);
				Assert.assertTrue(carrierInformationElement.getVehicleDepots().length == 2);
				Assert.assertTrue(carrierInformationElement.getVehicleTypes().length == 2);
			}
			else if(carrierInformationElement.getName().equals("testCarrier3")) {
				Assert.assertTrue(carrierInformationElement.getAreaOfAdditonalDepots() == null);
				Assert.assertTrue(carrierInformationElement.getFixedNumberOfVehilcePerTypeAndLocation() == 0);
				Assert.assertTrue(carrierInformationElement.getFleetSize() == FleetSize.INFINITE);
				Assert.assertTrue(carrierInformationElement.getJspritIterations() == 0);
				Assert.assertTrue(carrierInformationElement.getNumberOfDepotsPerType() == 2);
				Assert.assertTrue(carrierInformationElement.getVehicleStartTime() == 50000);
				Assert.assertTrue(carrierInformationElement.getVehicleEndTime() == 80000);
				Assert.assertTrue(carrierInformationElement.getVehicleDepots().length == 1);
				Assert.assertTrue(carrierInformationElement.getVehicleTypes().length == 1);
				Assert.assertTrue(carrierInformationElement.getVehicleTypes()[0].equals("testVehicle1"));
			}
			else if(carrierInformationElement.getName().equals("testCarrier2")) {
				Assert.assertTrue(carrierInformationElement.getAreaOfAdditonalDepots() == null);
				Assert.assertTrue(carrierInformationElement.getFixedNumberOfVehilcePerTypeAndLocation() == 3);
				Assert.assertTrue(carrierInformationElement.getFleetSize() == FleetSize.FINITE);
				Assert.assertTrue(carrierInformationElement.getJspritIterations() == 15);
				Assert.assertTrue(carrierInformationElement.getNumberOfDepotsPerType() == 3);
				Assert.assertTrue(carrierInformationElement.getVehicleStartTime() == 3600);
				Assert.assertTrue(carrierInformationElement.getVehicleEndTime() == 50000);
				Assert.assertTrue(carrierInformationElement.getVehicleDepots().length == 1);
				Assert.assertTrue(carrierInformationElement.getVehicleTypes().length == 1);
				Assert.assertTrue(carrierInformationElement.getVehicleTypes()[0].equals("testVehicle2"));
			}
			else {
				Assert.fail();
			}
		}

	}
}
