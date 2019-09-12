package org.matsim.contrib.freight.carrier;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.testcases.MatsimTestCase;

public class CarrierVehicleTypeLoaderTest extends MatsimTestCase{
	
	CarrierVehicleTypes types;
	
	Carriers carriers;
	
	@Override
	public void setUp() throws Exception{
		super.setUp();
		types = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader(types).readFile(getClassInputDirectory() + "vehicleTypes.xml");
		carriers = new Carriers();
		new CarrierPlanXmlReader(carriers).readFile(getClassInputDirectory() + "carrierPlansEquils.xml");
	}

	@Test
	public void test_whenLoadingTypes_allAssignmentsInLightVehicleAreCorrectly(){
		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(types);
		CarrierVehicle v = getVehicle("lightVehicle");
		assertNotNull(v.getType() );
		
	}
	
	@Test
	public void test_whenLoadingTypes_allAssignmentsInMediumVehicleAreCorrectly(){
		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(types);
		CarrierVehicle v = getVehicle("mediumVehicle");
		assertNotNull(v.getType() );
		
	}

	private CarrierVehicle getVehicle(String vehicleName) {
		for(CarrierVehicle v : carriers.getCarriers().get(Id.create("testCarrier", Carrier.class)).getCarrierCapabilities().getCarrierVehicles()){
			if(v.getId().toString().equals(vehicleName )){
				return v;
			}
		}
		return null;
	}
}
