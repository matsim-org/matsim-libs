package org.matsim.contrib.freight.carrier;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.vehicles.Vehicle;

public class CarrierPlanXmlReaderV2Test extends MatsimTestCase {

	private Carrier testCarrier;
	
	@Override
	public void setUp() throws Exception{
		super.setUp();
		Carriers carriers = new Carriers();
		String classInputDirectory = getClassInputDirectory();
		new CarrierPlanXmlReader(carriers).readFile(classInputDirectory + "carrierPlansEquils.xml" );
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
	public void test_whenReadingCarrierWithFiniteFleet_itSetsFleetSizeCorrectly(){
		Carriers carriers = new Carriers();
		String classInputDirectory = getClassInputDirectory();
		new CarrierPlanXmlReader(carriers).readFile(classInputDirectory + "carrierPlansEquilsFiniteFleet.xml" );
		assertEquals(FleetSize.FINITE, carriers.getCarriers().get(Id.create("testCarrier", Carrier.class)).getCarrierCapabilities().getFleetSize());
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
	public void test_readStream() {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		Carriers carriers = FreightUtils.addOrGetCarriers(scenario);

		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
				"<carriers>\n" +
				"  <carrier id=\"1\">\n" +
				"    <attributes>\n" +
				"      <attribute name=\"jspritIterations\" class=\"java.lang.Integer\">50</attribute>\n" +
				"    </attributes>\n" +
				"    <capabilities fleetSize=\"INFINITE\">\n" +
				"      <vehicles>\n" +
				"        <vehicle id=\"carrier_1_heavyVehicle\" depotLinkId=\"12\" typeId=\"heavy-20t\" earliestStart=\"06:00:00\" latestEnd=\"16:00:00\"/>\n" +
				"      </vehicles>\n" +
				"    </capabilities>\n" +
				"    <services>\n" +
				"      <service id=\"1\" to=\"31\" capacityDemand=\"2500\" earliestStart=\"04:00:00\" latestEnd=\"10:00:00\" serviceDuration=\"00:45:00\"/>\n" +
				"    </services>\n" +
				"  </carrier>\n" +
				"</carriers>\n";

		InputStream is = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

		new CarrierPlanXmlReader(carriers).readStream(is);

		Assert.assertEquals(1, carriers.getCarriers().size());
	}

}
