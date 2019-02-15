package org.matsim.contrib.freight.carrier;

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
	
	public void test_whenReadingTypes_nuOfTypesIsReadCorrectly(){
		assertEquals(2, types.getVehicleTypes().size());
	}
	
	public void test_whenReadingTypes_itReadyExactlyTheTypesFromFile(){
		assertTrue(types.getVehicleTypes().containsKey(Id.create("medium", VehicleType.class)));
		assertTrue(types.getVehicleTypes().containsKey(Id.create("light", VehicleType.class)));
		assertEquals(2, types.getVehicleTypes().size());
	}
	
	public void test_whenReadingTypeMedium_itReadsDescriptionCorrectly(){
		CarrierVehicleType medium = types.getVehicleTypes().get(Id.create("medium", VehicleType.class));
		assertEquals("Medium Vehicle", medium.getDescription());
	}

	public void test_whenReadingTypeMedium_itReadsCapacityCorrectly(){
		CarrierVehicleType medium = types.getVehicleTypes().get(Id.create("medium", VehicleType.class));
		assertEquals(30, medium.getCarrierVehicleCapacity());
	}
	
	public void test_whenReadingTypeMedium_itReadsCostInfoCorrectly(){
		CarrierVehicleType medium = types.getVehicleTypes().get(Id.create("medium", VehicleType.class));
		assertEquals(50.0, medium.getVehicleCostInformation().getFix(),0.01);
		assertEquals(0.4, medium.getVehicleCostInformation().getPerDistanceUnit(),0.01);
		assertEquals(30.0, medium.getVehicleCostInformation().getPerTimeUnit(),0.01);
	}
	
	public void test_whenReadingTypeMedium_itReadsEngineInfoCorrectly(){
		CarrierVehicleType medium = types.getVehicleTypes().get(Id.create("medium", VehicleType.class));
		assertEquals(0.02, medium.getEngineInformation().getGasConsumption(),0.01);
		assertEquals("gasoline", medium.getEngineInformation().getFuelType().toString());
	}
}
