package org.matsim.contrib.freight.carrier;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.junit.*;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.contrib.freight.controler.FreightUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

public class CarrierPlanXmlReaderV2Test {

	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	private Carrier testCarrier;

	@Before
	public void setUp() throws Exception{
		CarrierVehicleTypes carrierVehicleTypes = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader( carrierVehicleTypes ).readFile( utils.getPackageInputDirectory() + "vehicleTypes_v2.xml" );

		Carriers carriers = new Carriers();
		String classInputDirectory = utils.getClassInputDirectory();
		new CarrierPlanXmlReader(carriers, carrierVehicleTypes ).readFile(classInputDirectory + "carrierPlansEquils.xml" );
		testCarrier = carriers.getCarriers().get(Id.create("testCarrier", Carrier.class));
	}

	@Test
	public void test_whenReadingServices_nuOfServicesIsCorrect(){
		Assert.assertEquals(3,testCarrier.getServices().size());
	}

	@Test
	public void test_whenReadingCarrier_itReadsTypeIdsCorrectly(){

		CarrierVehicle light = CarrierUtils.getCarrierVehicle(testCarrier, Id.createVehicleId("lightVehicle"));
		Gbl.assertNotNull(light);
		Assert.assertEquals("light",light.getVehicleTypeId().toString());

		CarrierVehicle medium = CarrierUtils.getCarrierVehicle(testCarrier, Id.createVehicleId("mediumVehicle"));
		Gbl.assertNotNull(medium);
		Assert.assertEquals("medium",medium.getVehicleTypeId().toString());

		CarrierVehicle heavy = CarrierUtils.getCarrierVehicle(testCarrier, Id.createVehicleId("heavyVehicle"));
		Gbl.assertNotNull(heavy);
		Assert.assertEquals("heavy",heavy.getVehicleTypeId().toString());
	}

	@Test
	public void test_whenReadingCarrier_itReadsVehiclesCorrectly(){
		Map<Id<Vehicle>, CarrierVehicle> carrierVehicles = testCarrier.getCarrierCapabilities().getCarrierVehicles();
		Assert.assertEquals(3,carrierVehicles.size());
		Assert.assertTrue(exactlyTheseVehiclesAreInVehicleCollection(Arrays.asList(Id.create("lightVehicle", Vehicle.class),
				Id.create("mediumVehicle", Vehicle.class),Id.create("heavyVehicle", Vehicle.class)),carrierVehicles.values()));
	}

	@Test
	public void test_whenReadingCarrier_itReadsFleetSizeCorrectly(){
		Assert.assertEquals(FleetSize.INFINITE, testCarrier.getCarrierCapabilities().getFleetSize());
	}

	@Test
	public void test_whenReadingCarrier_itReadsShipmentsCorrectly(){
		Assert.assertEquals(2, testCarrier.getShipments().size());
	}

	@Test
	public void test_whenReadingCarrier_itReadsPlansCorrectly(){
		Assert.assertEquals(3, testCarrier.getPlans().size());
	}

	@Test
	public void test_whenReadingCarrier_itSelectsPlansCorrectly(){
		Assert.assertNotNull(testCarrier.getSelectedPlan());
	}

	@Test
	public void test_whenReadingCarrierWithFiniteFleet_itSetsFleetSizeCorrectly(){

		CarrierVehicleTypes carrierVehicleTypes = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader( carrierVehicleTypes ).readFile( utils.getPackageInputDirectory() + "vehicleTypes_v2.xml" );

		Carriers carriers = new Carriers();
		String classInputDirectory = utils.getClassInputDirectory();
		new CarrierPlanXmlReader(carriers, carrierVehicleTypes ).readFile(classInputDirectory + "carrierPlansEquilsFiniteFleet.xml" );
		Assert.assertEquals(FleetSize.FINITE, carriers.getCarriers().get(Id.create("testCarrier", Carrier.class)).getCarrierCapabilities().getFleetSize());
	}

	@Test
	public void test_whenReadingPlans_nuOfToursIsCorrect(){
		List<CarrierPlan> plans = new ArrayList<>(testCarrier.getPlans());
		Assert.assertEquals(1, plans.get(0).getScheduledTours().size());
		Assert.assertEquals(1, plans.get(1).getScheduledTours().size());
		Assert.assertEquals(1, plans.get(2).getScheduledTours().size());
	}

	@Test
	public void test_whenReadingToursOfPlan1_nuOfActivitiesIsCorrect(){
		List<CarrierPlan> plans = new ArrayList<>(testCarrier.getPlans());
		CarrierPlan plan1 = plans.get(0);
		ScheduledTour tour1 = plan1.getScheduledTours().iterator().next();
		Assert.assertEquals(5,tour1.getTour().getTourElements().size());
	}

	@Test
	public void test_whenReadingToursOfPlan2_nuOfActivitiesIsCorrect(){
		List<CarrierPlan> plans = new ArrayList<>(testCarrier.getPlans());
		CarrierPlan plan2 = plans.get(1);
		ScheduledTour tour1 = plan2.getScheduledTours().iterator().next();
		Assert.assertEquals(9,tour1.getTour().getTourElements().size());
	}

	@Test
	public void test_whenReadingToursOfPlan3_nuOfActivitiesIsCorrect(){
		List<CarrierPlan> plans = new ArrayList<>(testCarrier.getPlans());
		CarrierPlan plan3 = plans.get(2);
		ScheduledTour tour1 = plan3.getScheduledTours().iterator().next();
		Assert.assertEquals(9,tour1.getTour().getTourElements().size());
	}


	private boolean exactlyTheseVehiclesAreInVehicleCollection(List<Id<Vehicle>> asList, Collection<CarrierVehicle> carrierVehicles) {
		List<CarrierVehicle> vehicles = new ArrayList<>(carrierVehicles);
		for(CarrierVehicle type : carrierVehicles) if(asList.contains(type.getId() )) vehicles.remove(type );
		return vehicles.isEmpty();
	}


	@Test
	public void test_CarrierHasAttributes(){
		Assert.assertEquals((TransportMode.drt),CarrierUtils.getCarrierMode(testCarrier));
		Assert.assertEquals(50,CarrierUtils.getJspritIterations(testCarrier));
	}

	@Test
	public void test_ServicesAndShipmentsHaveAttributes(){
		Object serviceCustomerAtt = testCarrier.getServices().get(Id.create("serv1",CarrierService.class)).getAttributes().getAttribute("customer");
		Assert.assertNotNull(serviceCustomerAtt);
		Assert.assertEquals("someRandomCustomer", serviceCustomerAtt);
		Object shipmentCustomerAtt = testCarrier.getShipments().get(Id.create("s1",CarrierShipment.class)).getAttributes().getAttribute("customer");
		Assert.assertNotNull(shipmentCustomerAtt);
		Assert.assertEquals("someRandomCustomer", shipmentCustomerAtt);
	}

	@Test
	public void test_readStream() {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		Carriers carriers = FreightUtils.addOrGetCarriers(scenario);

		String xml = """
				<?xml version="1.0" encoding="UTF-8"?>
				<carriers>
				  <carrier id="1">
				    <attributes>
				      <attribute name="jspritIterations" class="java.lang.Integer">50</attribute>
				    </attributes>
				    <capabilities fleetSize="INFINITE">
				      <vehicles>
				        <vehicle id="carrier_1_heavyVehicle" depotLinkId="12" typeId="heavy-20t" earliestStart="06:00:00" latestEnd="16:00:00"/>
				      </vehicles>
				    </capabilities>
				    <services>
				      <service id="1" to="31" capacityDemand="2500" earliestStart="04:00:00" latestEnd="10:00:00" serviceDuration="00:45:00"/>
				    </services>
				  </carrier>
				</carriers>
				""";

		InputStream is = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

		CarrierVehicleTypes carrierVehicleTypes = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader( carrierVehicleTypes ).readFile( utils.getPackageInputDirectory() + "vehicleTypes_v2.xml" );
		// yyyy should rather construct in code.  kai, jan'22

		new CarrierPlanXmlReader(carriers, carrierVehicleTypes ).readStream(is );

		Assert.assertEquals(1, carriers.getCarriers().size());
	}

}
