package org.matsim.contrib.freight.carrier;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

public class CarrierVehicleTypeLoaderTest extends MatsimTestCase{


	private CarrierVehicleTypes types;
	private Carriers carriers;
	
	@Override
	public void setUp() throws Exception{
		super.setUp();
		types = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader(types).readFile(getClassInputDirectory() + "vehicleTypes.xml");
		carriers = new Carriers();
		new CarrierPlanXmlReader(carriers).readFile(getClassInputDirectory() + "carrierPlansEquils.xml" );
	}

	@Test
	public void test_whenLoadingTypes_allAssignmentsInLightVehicleAreCorrectly(){
		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(types);
		Carrier testCarrier = carriers.getCarriers().get(Id.create("testCarrier", Carrier.class));
		CarrierVehicle v = CarrierUtils.getCarrierVehicle(testCarrier,Id.createVehicleId("lightVehicle"));

		VehicleType vehicleTypeLoaded = v.getType();
		assertNotNull(vehicleTypeLoaded);

		assertEquals("light", vehicleTypeLoaded.getId().toString());
		assertEquals(15, vehicleTypeLoaded.getCapacity().getOther(), EPSILON);
		assertEquals(20, vehicleTypeLoaded.getCostInformation().getFixedCosts(), EPSILON);
		assertEquals(0.35, vehicleTypeLoaded.getCostInformation().getCostsPerMeter(), EPSILON);
		assertEquals(30, vehicleTypeLoaded.getCostInformation().getCostsPerSecond(), EPSILON);

		assertEquals("gasoline", vehicleTypeLoaded.getEngineInformation().getFuelType().toString());
		assertEquals(0.02, VehicleUtils.getFuelConsumption(vehicleTypeLoaded), EPSILON);
	}
	
	@Test
	public void test_whenLoadingTypes_allAssignmentsInMediumVehicleAreCorrectly(){
		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(types);
		Carrier testCarrier = carriers.getCarriers().get(Id.create("testCarrier", Carrier.class));
		CarrierVehicle v = CarrierUtils.getCarrierVehicle(testCarrier,Id.createVehicleId("mediumVehicle"));

		VehicleType vehicleTypeLoaded = v.getType();
		assertNotNull(vehicleTypeLoaded);

		assertEquals("medium", vehicleTypeLoaded.getId().toString());
		assertEquals(30, vehicleTypeLoaded.getCapacity().getOther(), EPSILON);
		assertEquals(50, vehicleTypeLoaded.getCostInformation().getFixedCosts(), EPSILON);
		assertEquals(0.4, vehicleTypeLoaded.getCostInformation().getCostsPerMeter(), EPSILON);
		assertEquals(30, vehicleTypeLoaded.getCostInformation().getCostsPerSecond(), EPSILON);

		assertEquals("gasoline", vehicleTypeLoaded.getEngineInformation().getFuelType().toString());
		assertEquals(0.02, VehicleUtils.getFuelConsumption(vehicleTypeLoaded), EPSILON);
		
	}

}
