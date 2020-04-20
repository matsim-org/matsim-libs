package org.matsim.contrib.freight.carrier;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

public class CarrierVehicleTypeLoaderTest extends MatsimTestCase{

	private static Logger log = Logger.getLogger(CarrierVehicleTypeLoaderTest.class);

	CarrierVehicleTypes types;
	Carriers carriers;
	
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
		assertNotNull(v.getType() );
		
	}
	
	@Test
	public void test_whenLoadingTypes_allAssignmentsInMediumVehicleAreCorrectly(){
		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(types);
		Carrier testCarrier = carriers.getCarriers().get(Id.create("testCarrier", Carrier.class));
		CarrierVehicle v = CarrierUtils.getCarrierVehicle(testCarrier,Id.createVehicleId("mediumVehicle"));
		assertNotNull(v.getType() );
		
	}

//	private CarrierVehicle getVehicle(String vehicleName) {
//		Id<Vehicle> vehicleId = Id.create(vehicleName, Vehicle.class);
//		Carrier testCarrier = carriers.getCarriers().get(Id.create("testCarrier", Carrier.class));
//		if(testCarrier.getCarrierCapabilities().getCarrierVehicles().containsKey(vehicleId)){
//			return testCarrier.getCarrierCapabilities().getCarrierVehicles().get(vehicleId);
//		}
//		log.error("Vehicle with Id does not exists", new IllegalStateException("vehicle with id " + vehicleId + " is missing"));
//		return null;
//	}
}
