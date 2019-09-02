package org.matsim.contrib.freight.carrier;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.testcases.MatsimTestUtils;

public class CarrierVehicleTypeWriterTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils() ;
	
	@Test @Ignore
	public void testTypeWriter(){
		CarrierVehicleTypes types = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader(types).readFile(utils.getClassInputDirectory()+ "vehicleTypes.xml");
		final String outputVehTypeFile = utils.getOutputDirectory()+ "vehicleTypesWritten.xml";
		new CarrierVehicleTypeWriter(types).write(outputVehTypeFile);
		types.getVehicleTypes().clear();
		new CarrierVehicleTypeReader(types).readFile(outputVehTypeFile);
	}
}
