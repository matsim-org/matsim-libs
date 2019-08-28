package org.matsim.contrib.freight.carrier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.vehicles.Vehicle;

public class CarrierPlanXmlReaderV2Test extends MatsimTestCase {
	
	Carrier testCarrier;
	
	@Override
	public void setUp() throws Exception{
		super.setUp();
		Carriers carriers = new Carriers();
		String classInputDirectory = getClassInputDirectory();
		new CarrierPlanXmlReader(carriers).readFile(classInputDirectory + "carrierPlansEquils.xml");
		testCarrier = carriers.getCarriers().get(Id.create("testCarrier", Carrier.class));
	}
	
	@Test
	public void test_whenReadingServices_nuOfServicesIsCorrect(){
		assertEquals(3,testCarrier.getServices().size());
	}
	
	@Test
	public void test_whenReadingCarrier_itReadsTypeIdsCorrectly(){
		
		CarrierVehicle light = getVehicle("lightVehicle");
		assertEquals("light",light.getVehicleTypeId().toString());
		
		CarrierVehicle medium = getVehicle("mediumVehicle");
		assertEquals("medium",medium.getVehicleTypeId().toString());
		
		CarrierVehicle heavy = getVehicle("heavyVehicle");
		assertEquals("heavy",heavy.getVehicleTypeId().toString());
	}
	
	@Test
	public void test_whenReadingCarrier_itReadsVehiclesCorrectly(){
		Collection<CarrierVehicle> carrierVehicles = testCarrier.getCarrierCapabilities().getCarrierVehicles();
		assertEquals(3,carrierVehicles.size());
		assertTrue(exactlyTheseVehiclesAreInVehicleCollection(Arrays.asList(Id.create("lightVehicle", Vehicle.class),
				Id.create("mediumVehicle", Vehicle.class),Id.create("heavyVehicle", Vehicle.class)),carrierVehicles));
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
		new CarrierPlanXmlReader(carriers).readFile(classInputDirectory + "carrierPlansEquilsFiniteFleet.xml");
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

	private CarrierVehicle getVehicle(String vehicleName) {
		for(CarrierVehicle v : testCarrier.getCarrierCapabilities().getCarrierVehicles()){
			if(v.getId().toString().equals(vehicleName )){
				return v;
			}
		}
		return null;
	}
	
}
