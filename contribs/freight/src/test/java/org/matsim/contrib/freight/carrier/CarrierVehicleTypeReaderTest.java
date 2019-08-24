package org.matsim.contrib.freight.carrier;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.vehicles.VehicleType;

public class CarrierVehicleTypeReaderTest extends MatsimTestCase{
	
	CarrierVehicleTypes types;
	
	@Override
	public void setUp() throws Exception{
		super.setUp();
		types = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader(types).readFile(getClassInputDirectory() + "vehicleTypes.xml");
	}
	
	@Test
	public void test_whenReadingTypes_nuOfTypesIsReadCorrectly(){
		assertEquals(2, types.getVehicleTypes().size());
	}
	
	@Test
	public void test_whenReadingTypes_itReadyExactlyTheTypesFromFile(){
		assertTrue(types.getVehicleTypes().containsKey(Id.create("medium", VehicleType.class)));
		assertTrue(types.getVehicleTypes().containsKey(Id.create("light", VehicleType.class)));
		assertEquals(2, types.getVehicleTypes().size());
	}
	
	@Test
	public void test_whenReadingTypeMedium_itReadsDescriptionCorrectly(){
		CarrierVehicleType medium = types.getVehicleTypes().get(Id.create("medium", VehicleType.class));
		assertEquals("Medium Vehicle", medium.getDescription());
	}

	@Test
	public void test_whenReadingTypeMedium_itReadsCapacityCorrectly(){
		CarrierVehicleType medium = types.getVehicleTypes().get(Id.create("medium", VehicleType.class));
		assertEquals(30., medium.getCapacity().getWeightInTons() );
	}
	
	@Test
	public void test_whenReadingTypeMedium_itReadsCostInfoCorrectly(){
		CarrierVehicleType medium = types.getVehicleTypes().get(Id.create("medium", VehicleType.class));
		assertEquals(50.0, medium.getCostInformation().getFixedCosts(),0.01);
		assertEquals(0.4, medium.getCostInformation().getCostsPerMeter(),0.01);
		assertEquals(30.0, medium.getCostInformation().getCostsPerSecond(),0.01);
	}
	
	@Test
	public void test_whenReadingTypeMedium_itReadsEngineInfoCorrectly(){
		CarrierVehicleType medium = types.getVehicleTypes().get(Id.create("medium", VehicleType.class));
		assertEquals(0.02, medium.getEngineInformation().getFuelConsumption(),0.01);
		assertEquals("gasoline", medium.getEngineInformation().getFuelType().toString());
	}
}
