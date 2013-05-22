package org.matsim.contrib.freight.carrier;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestCase;

public class CarrierVehicleTypeLoaderTest extends MatsimTestCase{
	
	CarrierVehicleTypes types;
	
	Carriers carriers;
	
	public void setUp() throws Exception{
		super.setUp();
		types = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader(types).read(getClassInputDirectory() + "vehicleTypes.xml");
		carriers = new Carriers();
		new CarrierPlanXmlReaderV2(carriers).read(getClassInputDirectory() + "carrierPlansEquils.xml");
	}

	public void test_whenLoadingTypes_allAssignmentsInLightVehicleAreCorrectly(){
		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(types);
		CarrierVehicle v = getVehicle("lightVehicle");
		assertNotNull(v.getVehicleType());
		
	}
	
	public void test_whenLoadingTypes_allAssignmentsInMediumVehicleAreCorrectly(){
		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(types);
		CarrierVehicle v = getVehicle("mediumVehicle");
		assertNotNull(v.getVehicleType());
		
	}

	private CarrierVehicle getVehicle(String vehicleName) {
		for(CarrierVehicle v : carriers.getCarriers().get(new IdImpl("testCarrier")).getCarrierCapabilities().getCarrierVehicles()){
			if(v.getVehicleId().toString().equals(vehicleName)){
				return v;
			}
		}
		return null;
	}
}
