package org.matsim.contrib.freight.carrier;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.vehicles.CostInformation;
import org.matsim.vehicles.EngineInformation.FuelType;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.VehicleType;

public class CarrierVehicleTypeTest extends MatsimTestCase{

	CarrierVehicleTypes types;

	@Override
	public void setUp() throws Exception{
		super.setUp();
		VehicleType mediumType = CarrierUtils.CarrierVehicleTypeBuilder.newInstance(Id.create("medium", org.matsim.vehicles.VehicleType.class ) )
												   .setDescription("Medium Vehicle")
												   .setCapacityWeightInTons(30 )
												   .setVehicleCostInformation(new CostInformation(50., 1.0, 0.5) )
												   .setEngineInformation(new EngineInformation(FuelType.diesel, 0.02) )
												   .setMaxVelocity(13.89)
												   .build();
		types = new CarrierVehicleTypes();
		types.getVehicleTypes().put(mediumType.getId(), mediumType);

		//Setting up a copy of the one above
		VehicleType mediumType2 = CarrierUtils.CarrierVehicleTypeBuilder.newInstance(Id.create("medium2", org.matsim.vehicles.VehicleType.class ), mediumType ).build();
		types.getVehicleTypes().put(mediumType2.getId(), mediumType2);

		//Setting up a smaller one based of the one above and changing all values.
		VehicleType smallType = CarrierUtils.CarrierVehicleTypeBuilder.newInstance(Id.create("small", org.matsim.vehicles.VehicleType.class ), mediumType )
												  .setDescription("Small Vehicle")
												  .setCapacityWeightInTons(16 )
												  .setVehicleCostInformation(new CostInformation(25., 0.75, 0.25) )
												  .setEngineInformation(new EngineInformation(FuelType.gasoline, 0.015) )
												  .setMaxVelocity(10.0)
												  .build();
		types.getVehicleTypes().put(smallType.getId(), smallType);
	}

	@Test
	public void test_whenCreatingTypeMedium_itCreatesDescriptionCorrectly(){
		VehicleType medium = types.getVehicleTypes().get(Id.create("medium", org.matsim.vehicles.VehicleType.class ) );
		assertEquals("Medium Vehicle", medium.getDescription());
	}

	@Test
	public void test_whenCreatingTypeMedium_itCreatesCapacityCorrectly(){
		VehicleType medium = types.getVehicleTypes().get(Id.create("medium", org.matsim.vehicles.VehicleType.class ) );
		assertEquals(30., medium.getCapacity().getWeightInTons() );
	}

	@Test
	public void test_whenCreatingTypeMedium_itCreatesCostInfoCorrectly(){
		VehicleType medium = types.getVehicleTypes().get(Id.create("medium", org.matsim.vehicles.VehicleType.class ) );
		assertEquals(50.0, medium.getCostInformation().getFixedCosts(),0.01);
		assertEquals(1.0, medium.getCostInformation().getCostsPerMeter(),0.01);
		assertEquals(0.5, medium.getCostInformation().getCostsPerSecond(),0.01);
	}

	@Test
	public void test_whenCreatingTypeMedium_itCreatesEngineInfoCorrectly(){
		VehicleType medium = types.getVehicleTypes().get(Id.create("medium", org.matsim.vehicles.VehicleType.class ) );
		assertEquals(0.02, medium.getEngineInformation().getFuelConsumption(),0.001);
		assertEquals(FuelType.diesel, medium.getEngineInformation().getFuelType());
	}

	@Test
	public void test_whenCreatingTypeMedium_itCreatesMaxVelocityCorrectly(){
		VehicleType medium = types.getVehicleTypes().get(Id.create("medium", org.matsim.vehicles.VehicleType.class ) );
		assertEquals(13.89, medium.getMaximumVelocity(), 0.01);
	}

	//Now testing the copy
	@Test
	public void test_whenCopyingTypeMedium_itCopiesDescriptionCorrectly(){
		VehicleType medium2 = types.getVehicleTypes().get(Id.create("medium2", org.matsim.vehicles.VehicleType.class ) );
		assertEquals("Medium Vehicle", medium2.getDescription());
	}

	@Test
	public void test_whenCopyingTypeMedium_itCopiesCapacityCorrectly(){
		VehicleType medium2 = types.getVehicleTypes().get(Id.create("medium2", org.matsim.vehicles.VehicleType.class ) );
		assertEquals(30., medium2.getCapacity().getWeightInTons() );
	}

	@Test
	public void test_whenCopyingTypeMedium_itCopiesCostInfoCorrectly(){
		VehicleType medium2 = types.getVehicleTypes().get(Id.create("medium2", org.matsim.vehicles.VehicleType.class ) );
		assertEquals(50.0, medium2.getCostInformation().getFixedCosts(),0.01);
		assertEquals(1.0, medium2.getCostInformation().getCostsPerMeter(),0.01);
		assertEquals(0.5, medium2.getCostInformation().getCostsPerSecond(),0.01);
	}

	@Test
	public void test_whenCopyingTypeMedium_itCopiesEngineInfoCorrectly(){
		VehicleType medium2 = types.getVehicleTypes().get(Id.create("medium2", org.matsim.vehicles.VehicleType.class ) );
		assertEquals(0.02, medium2.getEngineInformation().getFuelConsumption(),0.001);
		assertEquals(FuelType.diesel, medium2.getEngineInformation().getFuelType());
	}

	@Test
	public void test_whenCopyingTypemMedium_itCopiesMaxVelocityCorrectly(){
		VehicleType medium2 = types.getVehicleTypes().get(Id.create("medium2", org.matsim.vehicles.VehicleType.class ) );
		assertEquals(13.89, medium2.getMaximumVelocity(), 0.01);
	}

	//Now testing the modified type.
	@Test
	public void test_whenModifyingTypesmall_itModifiesDescriptionCorrectly(){
		VehicleType small = types.getVehicleTypes().get(Id.create("small", org.matsim.vehicles.VehicleType.class ) );
		assertEquals("Small Vehicle", small.getDescription());
	}

	@Test
	public void test_whenModifyingTypesmall_itModifiesCapacityCorrectly(){
		VehicleType small = types.getVehicleTypes().get(Id.create("small", org.matsim.vehicles.VehicleType.class ) );
		assertEquals(16., small.getCapacity().getWeightInTons() );
	}

	@Test
	public void test_whenModifyingTypesmall_itModifiesCostInfoCorrectly(){
		VehicleType small = types.getVehicleTypes().get(Id.create("small", org.matsim.vehicles.VehicleType.class ) );
		assertEquals(25.0, small.getCostInformation().getFixedCosts(),0.01);
		assertEquals(0.75, small.getCostInformation().getCostsPerMeter(),0.01);
		assertEquals(0.25, small.getCostInformation().getCostsPerSecond(),0.01);
	}

	@Test
	public void test_whenModifyingTypesmall_itModifiesEngineInfoCorrectly(){
		VehicleType small = types.getVehicleTypes().get(Id.create("small", org.matsim.vehicles.VehicleType.class ) );
		assertEquals(0.015, small.getEngineInformation().getFuelConsumption(),0.001);
		assertEquals(FuelType.gasoline, small.getEngineInformation().getFuelType());
	}

	@Test
	public void test_whenModifyingTypesmall_itModifiesMaxVelocityCorrectly(){
		VehicleType small = types.getVehicleTypes().get(Id.create("small", org.matsim.vehicles.VehicleType.class ) );
		assertEquals(10.0, small.getMaximumVelocity(), 0.01);
	}


}
