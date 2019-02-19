package org.matsim.contrib.freight.carrier;

import org.junit.Test;
import org.matsim.testcases.MatsimTestCase;

public class CarrierVehicleTypeWriterTest extends MatsimTestCase{
	
	@Test
	public void testTypeWriter(){
		CarrierVehicleTypes types = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader(types).readFile(getClassInputDirectory() + "vehicleTypes.xml");
		new CarrierVehicleTypeWriter(types).write(getClassInputDirectory() + "vehicleTypesWritten.xml");
		types.getVehicleTypes().clear();
		new CarrierVehicleTypeReader(types).readFile(getClassInputDirectory() + "vehicleTypesWritten.xml");
	}
}
