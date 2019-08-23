package org.matsim.contrib.freight.carrier;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.vehicles.CostInformationImpl;
import org.matsim.vehicles.EngineInformation.FuelType;
import org.matsim.vehicles.EngineInformationImpl;
import org.matsim.vehicles.VehicleType;

public class CarrierVehicleTypeTest extends MatsimTestCase{

	CarrierVehicleTypes types;

	@Override
	public void setUp() throws Exception{
		super.setUp();
		CarrierVehicleType mediumType = CarrierVehicleType.Builder.newInstance(Id.create("medium", VehicleType.class ))
				.setDescription("Medium Vehicle")
				.setCapacityWeightInTons(30 )
				.setVehicleCostInformation(new CostInformationImpl(50., 1.0, 0.5))
				.setEngineInformation(new EngineInformationImpl(FuelType.diesel, 0.02))
				.setMaxVelocity(13.89)
				.build();
		types = new CarrierVehicleTypes();
		types.getVehicleTypes().put(mediumType.getId(), mediumType);

		//Setting up a copy of the one above
		CarrierVehicleType mediumType2 = CarrierVehicleType.Builder.newInstance(Id.create("medium2", VehicleType.class), mediumType).build();
		types.getVehicleTypes().put(mediumType2.getId(), mediumType2);

		//Setting up a smaller one based of the one above and changing all values.
		CarrierVehicleType smallType = CarrierVehicleType.Builder.newInstance(Id.create("small", VehicleType.class), mediumType)
				.setDescription("Small Vehicle")
				.setCapacityWeightInTons(16 )
				.setVehicleCostInformation(new CostInformationImpl(25., 0.75, 0.25))
				.setEngineInformation(new EngineInformationImpl(FuelType.gasoline, 0.015))
				.setMaxVelocity(10.0)
				.build();
		types.getVehicleTypes().put(smallType.getId(), smallType);
	}

	@Test
	public void test_whenCreatingTypeMedium_itCreatesDescriptionCorrectly(){
		CarrierVehicleType medium = types.getVehicleTypes().get(Id.create("medium", VehicleType.class));
		assertEquals("Medium Vehicle", medium.getDescription());
	}

	@Test
	public void test_whenCreatingTypeMedium_itCreatesCapacityCorrectly(){
		CarrierVehicleType medium = types.getVehicleTypes().get(Id.create("medium", VehicleType.class));
		assertEquals(30, medium.getCarrierVehicleCapacity());
	}

	@Test
	public void test_whenCreatingTypeMedium_itCreatesCostInfoCorrectly(){
		CarrierVehicleType medium = types.getVehicleTypes().get(Id.create("medium", VehicleType.class));
		assertEquals(50.0, medium.getCostInformation().getFixedCosts(),0.01);
		assertEquals(1.0, medium.getCostInformation().getCostsPerMeter(),0.01);
		assertEquals(0.5, medium.getCostInformation().getCostsPerSecond(),0.01);
	}

	@Test
	public void test_whenCreatingTypeMedium_itCreatesEngineInfoCorrectly(){
		CarrierVehicleType medium = types.getVehicleTypes().get(Id.create("medium", VehicleType.class));
		assertEquals(0.02, medium.getEngineInformation().getFuelConsumption(),0.001);
		assertEquals(FuelType.diesel, medium.getEngineInformation().getFuelType());
	}

	@Test
	public void test_whenCreatingTypeMedium_itCreatesMaxVelocityCorrectly(){
		CarrierVehicleType medium = types.getVehicleTypes().get(Id.create("medium", VehicleType.class));
		assertEquals(13.89, medium.getMaximumVelocity(), 0.01);
	}

	//Now testing the copy
	@Test
	public void test_whenCopyingTypeMedium_itCopiesDescriptionCorrectly(){
		CarrierVehicleType medium2 = types.getVehicleTypes().get(Id.create("medium2", VehicleType.class));
		assertEquals("Medium Vehicle", medium2.getDescription());
	}

	@Test
	public void test_whenCopyingTypeMedium_itCopiesCapacityCorrectly(){
		CarrierVehicleType medium2 = types.getVehicleTypes().get(Id.create("medium2", VehicleType.class));
		assertEquals(30, medium2.getCarrierVehicleCapacity());
	}

	@Test
	public void test_whenCopyingTypeMedium_itCopiesCostInfoCorrectly(){
		CarrierVehicleType medium2 = types.getVehicleTypes().get(Id.create("medium2", VehicleType.class));
		assertEquals(50.0, medium2.getCostInformation().getFixedCosts(),0.01);
		assertEquals(1.0, medium2.getCostInformation().getCostsPerMeter(),0.01);
		assertEquals(0.5, medium2.getCostInformation().getCostsPerSecond(),0.01);
	}

	@Test
	public void test_whenCopyingTypeMedium_itCopiesEngineInfoCorrectly(){
		CarrierVehicleType medium2 = types.getVehicleTypes().get(Id.create("medium2", VehicleType.class));
		assertEquals(0.02, medium2.getEngineInformation().getFuelConsumption(),0.001);
		assertEquals(FuelType.diesel, medium2.getEngineInformation().getFuelType());
	}

	@Test
	public void test_whenCopyingTypemMedium_itCopiesMaxVelocityCorrectly(){
		CarrierVehicleType medium2 = types.getVehicleTypes().get(Id.create("medium2", VehicleType.class));
		assertEquals(13.89, medium2.getMaximumVelocity(), 0.01);
	}

	//Now testing the modified type.
	@Test
	public void test_whenModifyingTypesmall_itModifiesDescriptionCorrectly(){
		CarrierVehicleType small = types.getVehicleTypes().get(Id.create("small", VehicleType.class));
		assertEquals("Small Vehicle", small.getDescription());
	}

	@Test
	public void test_whenModifyingTypesmall_itModifiesCapacityCorrectly(){
		CarrierVehicleType small = types.getVehicleTypes().get(Id.create("small", VehicleType.class));
		assertEquals(16, small.getCarrierVehicleCapacity());
	}

	@Test
	public void test_whenModifyingTypesmall_itModifiesCostInfoCorrectly(){
		CarrierVehicleType small = types.getVehicleTypes().get(Id.create("small", VehicleType.class));
		assertEquals(25.0, small.getCostInformation().getFixedCosts(),0.01);
		assertEquals(0.75, small.getCostInformation().getCostsPerMeter(),0.01);
		assertEquals(0.25, small.getCostInformation().getCostsPerSecond(),0.01);
	}

	@Test
	public void test_whenModifyingTypesmall_itModifiesEngineInfoCorrectly(){
		CarrierVehicleType small = types.getVehicleTypes().get(Id.create("small", VehicleType.class));
		assertEquals(0.015, small.getEngineInformation().getFuelConsumption(),0.001);
		assertEquals(FuelType.gasoline, small.getEngineInformation().getFuelType());
	}

	@Test
	public void test_whenModifyingTypesmall_itModifiesMaxVelocityCorrectly(){
		CarrierVehicleType small = types.getVehicleTypes().get(Id.create("small", VehicleType.class));
		assertEquals(10.0, small.getMaximumVelocity(), 0.01);
	}


}
