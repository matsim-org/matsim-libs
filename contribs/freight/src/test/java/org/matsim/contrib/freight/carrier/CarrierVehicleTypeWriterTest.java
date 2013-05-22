package org.matsim.contrib.freight.carrier;

import org.matsim.testcases.MatsimTestCase;

public class CarrierVehicleTypeWriterTest extends MatsimTestCase{
	
	
	public void testTypeWriter(){
		CarrierVehicleTypes types = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader(types).read(getClassInputDirectory() + "vehicleTypes.xml");
		new CarrierVehicleTypeWriter(types).write(getClassInputDirectory() + "vehicleTypesWritten.xml");
		types.getVehicleTypes().clear();
		new CarrierVehicleTypeReader(types).read(getClassInputDirectory() + "vehicleTypesWritten.xml");
	}
}
