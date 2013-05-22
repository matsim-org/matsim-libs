package org.matsim.contrib.freight.carrier;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestCase;

public class CarrierVehicleTypeReaderTest extends MatsimTestCase{
	
	CarrierVehicleTypes types;
	
	public void setUp() throws Exception{
		super.setUp();
		types = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader(types).read(getClassInputDirectory() + "vehicleTypes.xml");
	}
	
	public void test_whenReadingTypes_nuOfTypesIsReadCorrectly(){
		assertEquals(2, types.getVehicleTypes().size());
	}
	
	public void test_whenReadingTypes_itReadyExactlyTheTypesFromFile(){
		assertTrue(types.getVehicleTypes().containsKey(new IdImpl("medium")));
		assertTrue(types.getVehicleTypes().containsKey(new IdImpl("light")));
		assertEquals(2, types.getVehicleTypes().size());
	}
	
	public void test_whenReadingTypeMedium_itReadsDescriptionCorrectly(){
		CarrierVehicleType medium = types.getVehicleTypes().get(new IdImpl("medium"));
		assertEquals("Medium Vehicle", medium.getDescription());
	}

	public void test_whenReadingTypeMedium_itReadsCapacityCorrectly(){
		CarrierVehicleType medium = types.getVehicleTypes().get(new IdImpl("medium"));
		assertEquals(30, medium.getCarrierVehicleCapacity());
	}
	
	public void test_whenReadingTypeMedium_itReadsCostInfoCorrectly(){
		CarrierVehicleType medium = types.getVehicleTypes().get(new IdImpl("medium"));
		assertEquals(50.0, medium.getVehicleCostInformation().fix,0.01);
		assertEquals(0.4, medium.getVehicleCostInformation().perDistanceUnit,0.01);
		assertEquals(30.0, medium.getVehicleCostInformation().perTimeUnit,0.01);
	}
	
	public void test_whenReadingTypeMedium_itReadsEngineInfoCorrectly(){
		CarrierVehicleType medium = types.getVehicleTypes().get(new IdImpl("medium"));
		assertEquals(0.02, medium.getEngineInformation().getGasConsumption(),0.01);
		assertEquals("gasoline", medium.getEngineInformation().getFuelType().toString());
	}
}
