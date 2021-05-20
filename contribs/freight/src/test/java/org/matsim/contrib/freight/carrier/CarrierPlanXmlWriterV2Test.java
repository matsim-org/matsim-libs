package org.matsim.contrib.freight.carrier;

import java.util.*;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

import static org.junit.Assert.*;

public class CarrierPlanXmlWriterV2Test {

	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	private Carrier testCarrier;
	
	@Before
	public void setUp() throws Exception{
		Carriers carriers = new Carriers();
		String classInputDirectory = this.testUtils.getClassInputDirectory();
		new CarrierPlanXmlReader(carriers).readFile(classInputDirectory + "carrierPlansEquils.xml" );
		new CarrierPlanXmlWriterV2(carriers).write(this.testUtils.getClassInputDirectory() + "carrierPlansEquilsWritten.xml");
		carriers.getCarriers().clear();
		new CarrierPlanXmlReader(carriers).readFile(this.testUtils.getClassInputDirectory() + "carrierPlansEquilsWritten.xml" );
		testCarrier = carriers.getCarriers().get(Id.create("testCarrier", Carrier.class));
	}
	
	@Test
	public void test_whenReadingServices_nuOfServicesIsCorrect(){
		assertEquals(3,testCarrier.getServices().size());
	}
	
	@Test
	public void test_whenReadingCarrier_itReadsTypeIdsCorrectly(){

		CarrierVehicle light = CarrierUtils.getCarrierVehicle(testCarrier, Id.createVehicleId("lightVehicle"));
		assertEquals("light",light.getVehicleTypeId().toString());

		CarrierVehicle medium = CarrierUtils.getCarrierVehicle(testCarrier, Id.createVehicleId("mediumVehicle"));
		assertEquals("medium",medium.getVehicleTypeId().toString());

		CarrierVehicle heavy = CarrierUtils.getCarrierVehicle(testCarrier, Id.createVehicleId("heavyVehicle"));
		assertEquals("heavy",heavy.getVehicleTypeId().toString());
	}
	
	@Test
	public void test_whenReadingCarrier_itReadsVehiclesCorrectly(){
		Map<Id<Vehicle>, CarrierVehicle> carrierVehicles = testCarrier.getCarrierCapabilities().getCarrierVehicles();
		assertEquals(3,carrierVehicles.size());
		assertTrue(exactlyTheseVehiclesAreInVehicleCollection(Arrays.asList(Id.create("lightVehicle", Vehicle.class),
				Id.create("mediumVehicle", Vehicle.class),Id.create("heavyVehicle", Vehicle.class)),carrierVehicles.values()));
	}
	
	@Test
	public void test_whenReadingCarrier_itReadsFleetSizeCorrectly(){
		assertEquals(FleetSize.INFINITE, testCarrier.getCarrierCapabilities().getFleetSize());
	}
	
	@Test
	public void test_whenReadingCarrier_itReadsShipmentsCorrectly(){
		assertEquals(2, testCarrier.getShipments().size());
	}
	
	@Test
	public void test_whenReadingCarrier_itReadsPlansCorrectly(){
		assertEquals(3, testCarrier.getPlans().size());
	}
	
	@Test
	public void test_whenReadingCarrier_itSelectsPlansCorrectly(){
		assertNotNull(testCarrier.getSelectedPlan());
	}
	
	
	@Test
	public void test_whenReadingPlans_nuOfToursIsCorrect(){
		List<CarrierPlan> plans = new ArrayList<CarrierPlan>(testCarrier.getPlans());
		assertEquals(1, plans.get(0).getScheduledTours().size());
		assertEquals(1, plans.get(1).getScheduledTours().size());
		assertEquals(1, plans.get(2).getScheduledTours().size());
	}
	
	@Test
	public void test_whenReadingToursOfPlan1_nuOfActivitiesIsCorrect(){
		List<CarrierPlan> plans = new ArrayList<CarrierPlan>(testCarrier.getPlans());
		CarrierPlan plan1 = plans.get(0);
		ScheduledTour tour1 = plan1.getScheduledTours().iterator().next();
		assertEquals(5,tour1.getTour().getTourElements().size());
	}
	
	@Test
	public void test_whenReadingToursOfPlan2_nuOfActivitiesIsCorrect(){
		List<CarrierPlan> plans = new ArrayList<CarrierPlan>(testCarrier.getPlans());
		CarrierPlan plan2 = plans.get(1);
		ScheduledTour tour1 = plan2.getScheduledTours().iterator().next();
		assertEquals(9,tour1.getTour().getTourElements().size());
	}
	
	@Test
	public void test_whenReadingToursOfPlan3_nuOfActivitiesIsCorrect(){
		List<CarrierPlan> plans = new ArrayList<CarrierPlan>(testCarrier.getPlans());
		CarrierPlan plan3 = plans.get(2);
		ScheduledTour tour1 = plan3.getScheduledTours().iterator().next();
		assertEquals(9,tour1.getTour().getTourElements().size());
	}
	
	
	private boolean exactlyTheseVehiclesAreInVehicleCollection(List<Id<Vehicle>> asList, Collection<CarrierVehicle> carrierVehicles) {
		List<CarrierVehicle> vehicles = new ArrayList<CarrierVehicle>(carrierVehicles);
		for(CarrierVehicle type : carrierVehicles) if(asList.contains(type.getId() )) vehicles.remove(type );
		return vehicles.isEmpty();
	}

//	private CarrierVehicle getVehicle(String vehicleName) {
//		Id<Vehicle> vehicleId = Id.create(vehicleName, Vehicle.class);
//		if(testCarrier.getCarrierCapabilities().getCarrierVehicles().containsKey(vehicleId)){
//			return testCarrier.getCarrierCapabilities().getCarrierVehicles().get(vehicleId);
//		}
//		log.error("Vehicle with Id does not exists", new IllegalStateException("vehicle with id " + vehicleId + " is missing"));
//		return null;
//	}

	@Test
	public void test_CarrierHasAttributes(){
		assertEquals((TransportMode.drt),CarrierUtils.getCarrierMode(testCarrier));
		assertEquals(50,CarrierUtils.getJspritIterations(testCarrier));
	}

	@Test
	public void test_ServicesAndShipmentsHaveAttributes(){
		Object serviceCustomerAtt = testCarrier.getServices().get(Id.create("serv1",CarrierService.class)).getAttributes().getAttribute("customer");
		assertNotNull(serviceCustomerAtt);
		assertEquals("someRandomCustomer", (String) serviceCustomerAtt);
		Object shipmentCustomerAtt = testCarrier.getShipments().get(Id.create("s1",CarrierShipment.class)).getAttributes().getAttribute("customer");
		assertNotNull(shipmentCustomerAtt);
		assertEquals("someRandomCustomer", (String) shipmentCustomerAtt);
	}

	@Test
	public void test_properErrorWhenVehicleTypeIdIsMissing() {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		Carriers carriers = FreightUtils.addOrGetCarriers(scenario);

		Carrier carrier1 = CarrierUtils.createCarrier(Id.create("1", Carrier.class));
		CarrierUtils.setJspritIterations(carrier1, 50);

		CarrierVehicle c1hv = new CarrierVehicle.Builder(Id.create("carrier_1_heavyVehicle", Vehicle.class), Id.create("3", Link.class))
				// don't use setType() or setTypeId()
				.setEarliestStart(6 * 3600)
				.setLatestEnd(16 * 3600)
				.build();

		carrier1.getCarrierCapabilities().setFleetSize(FleetSize.INFINITE);
		carrier1.getCarrierCapabilities().getCarrierVehicles().put(c1hv.getId(), c1hv);

		carriers.addCarrier(carrier1);

		String outputDir = this.testUtils.getOutputDirectory();
		try {
			new CarrierPlanXmlWriterV2(carriers).write(outputDir + "/carriers.xml");
			Assert.fail("expected exception about missing vehicle type.");
		} catch (IllegalStateException e) {
			assertTrue(e.getMessage().contains("vehicleTypeId is missing"));
		}

	}

}
