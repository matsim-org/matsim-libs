package org.matsim.contrib.freight.carrier;

import org.matsim.testcases.MatsimTestCase;

public class CarrierVehicleTypeReaderTest extends MatsimTestCase{
	
	public void testReadingVehicleTypes(){
		CarrierVehicleTypes types = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader(types).read(getInputDirectory() + "vehicleTypes.xml");
		
	}

}
