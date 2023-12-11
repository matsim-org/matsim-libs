/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.freight.carriers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.freight.carriers.CarrierVehicleTypes;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.*;
import org.matsim.vehicles.EngineInformation.FuelType;

public class CarrierVehicleTypeTest {

	@RegisterExtension
	public final MatsimTestUtils utils = new MatsimTestUtils();

	CarrierVehicleTypes types;

	@BeforeEach
	public void setUp() throws Exception{
		final Id<VehicleType> vehicleTypeId = Id.create( "medium", VehicleType.class );
		VehicleType mediumType = VehicleUtils.getFactory().createVehicleType( vehicleTypeId );
		{
			CostInformation costInformation1 = mediumType.getCostInformation();
			costInformation1.setFixedCost( 50. );
			costInformation1.setCostsPerMeter( 1.0 );
			costInformation1.setCostsPerSecond( 0.5 );
			EngineInformation engineInformation1 = mediumType.getEngineInformation();
			engineInformation1.setFuelType( FuelType.diesel );
			engineInformation1.setFuelConsumption( 0.02 );
			VehicleCapacity vehicleCapacity = mediumType.getCapacity();
			vehicleCapacity.setWeightInTons( 30 );
			mediumType.setDescription( "Medium Vehicle" ).setMaximumVelocity( 13.89 );
			types = new CarrierVehicleTypes();
			types.getVehicleTypes().put( mediumType.getId(), mediumType );
		}
		//Setting up a copy of the one above
		VehicleType newVehicleType1 = VehicleUtils.getFactory().createVehicleType( Id.create("medium2", VehicleType.class ) );
		VehicleUtils.copyFromTo( mediumType, newVehicleType1 );
		VehicleType mediumType2 = newVehicleType1;
		types.getVehicleTypes().put(mediumType2.getId(), mediumType2);

		//Setting up a smaller one based of the one above and changing all values.
		final Id<VehicleType> smallTypeId = Id.create( "small", VehicleType.class );
		VehicleType newVehicleType = VehicleUtils.getFactory().createVehicleType( smallTypeId );
		VehicleUtils.copyFromTo( mediumType, newVehicleType );
		VehicleType smallType = newVehicleType ;
		{
			CostInformation costInformation = smallType.getCostInformation() ;
			costInformation.setFixedCost( 25. );
			costInformation.setCostsPerMeter( 0.75 );
			costInformation.setCostsPerSecond( 0.25 );
			EngineInformation engineInformation = smallType.getEngineInformation() ;
			engineInformation.setFuelType( FuelType.gasoline );
			engineInformation.setFuelConsumption( 0.015 );
			VehicleCapacity capacity = smallType.getCapacity() ;
			capacity.setWeightInTons( 16 ) ;
//			VehicleType smallType = CarriersUtils.CarrierVehicleTypeBuilder.newInstance( smallTypeId, mediumType )
			smallType.setDescription( "Small Vehicle" ).setMaximumVelocity( 10.0 ) ;
			types.getVehicleTypes().put( smallType.getId(), smallType );
		}
	}

	@Test
	void test_whenCreatingTypeMedium_itCreatesDescriptionCorrectly(){
		VehicleType medium = types.getVehicleTypes().get(Id.create("medium", org.matsim.vehicles.VehicleType.class ) );
		Assertions.assertEquals("Medium Vehicle", medium.getDescription());
	}

	@Test
	void test_whenCreatingTypeMedium_itCreatesCapacityCorrectly(){
		VehicleType medium = types.getVehicleTypes().get(Id.create("medium", org.matsim.vehicles.VehicleType.class ) );
		Assertions.assertEquals(30., medium.getCapacity().getWeightInTons(), MatsimTestUtils.EPSILON );
	}

	@Test
	void test_whenCreatingTypeMedium_itCreatesCostInfoCorrectly(){
		VehicleType medium = types.getVehicleTypes().get(Id.create("medium", org.matsim.vehicles.VehicleType.class ) );
		Assertions.assertEquals(50.0, medium.getCostInformation().getFixedCosts(),0.01 );
		Assertions.assertEquals(1.0, medium.getCostInformation().getCostsPerMeter(),0.01 );
		Assertions.assertEquals(0.5, medium.getCostInformation().getCostsPerSecond(),0.01 );
	}

	@Test
	void test_whenCreatingTypeMedium_itCreatesEngineInfoCorrectly(){
		VehicleType medium = types.getVehicleTypes().get(Id.create("medium", org.matsim.vehicles.VehicleType.class ) );
		Assertions.assertEquals(0.02, medium.getEngineInformation().getFuelConsumption(),0.001);
		Assertions.assertEquals(FuelType.diesel, medium.getEngineInformation().getFuelType());
	}

	@Test
	void test_whenCreatingTypeMedium_itCreatesMaxVelocityCorrectly(){
		VehicleType medium = types.getVehicleTypes().get(Id.create("medium", org.matsim.vehicles.VehicleType.class ) );
		Assertions.assertEquals(13.89, medium.getMaximumVelocity(), 0.01);
	}

	//Now testing the copy
	@Test
	void test_whenCopyingTypeMedium_itCopiesDescriptionCorrectly(){
		VehicleType medium2 = types.getVehicleTypes().get(Id.create("medium2", org.matsim.vehicles.VehicleType.class ) );
		Assertions.assertEquals("Medium Vehicle", medium2.getDescription());
	}

	@Test
	void test_whenCopyingTypeMedium_itCopiesCapacityCorrectly(){
		VehicleType medium2 = types.getVehicleTypes().get(Id.create("medium2", org.matsim.vehicles.VehicleType.class ) );
		Assertions.assertEquals(30., medium2.getCapacity().getWeightInTons(), MatsimTestUtils.EPSILON );
	}

	@Test
	void test_whenCopyingTypeMedium_itCopiesCostInfoCorrectly(){
		VehicleType medium2 = types.getVehicleTypes().get(Id.create("medium2", org.matsim.vehicles.VehicleType.class ) );
		Assertions.assertEquals(50.0, medium2.getCostInformation().getFixedCosts(),0.01 );
		Assertions.assertEquals(1.0, medium2.getCostInformation().getCostsPerMeter(),0.01 );
		Assertions.assertEquals(0.5, medium2.getCostInformation().getCostsPerSecond(),0.01 );
	}

	@Test
	void test_whenCopyingTypeMedium_itCopiesEngineInfoCorrectly(){
		VehicleType medium2 = types.getVehicleTypes().get(Id.create("medium2", org.matsim.vehicles.VehicleType.class ) );
		Assertions.assertEquals(0.02, medium2.getEngineInformation().getFuelConsumption(),0.001);
		Assertions.assertEquals(FuelType.diesel, medium2.getEngineInformation().getFuelType());
	}

	@Test
	void test_whenCopyingTypeMedium_itCopiesMaxVelocityCorrectly(){
		VehicleType medium2 = types.getVehicleTypes().get(Id.create("medium2", org.matsim.vehicles.VehicleType.class ) );
		Assertions.assertEquals(13.89, medium2.getMaximumVelocity(), 0.01);
	}

	//Now testing the modified type.
	@Test
	void test_whenModifyingTypeSmall_itModifiesDescriptionCorrectly(){
		VehicleType small = types.getVehicleTypes().get(Id.create("small", org.matsim.vehicles.VehicleType.class ) );
		Assertions.assertEquals("Small Vehicle", small.getDescription());
	}

	@Test
	void test_whenModifyingTypeSmall_itModifiesCapacityCorrectly(){
		VehicleType small = types.getVehicleTypes().get(Id.create("small", org.matsim.vehicles.VehicleType.class ) );
		Assertions.assertEquals(16., small.getCapacity().getWeightInTons(), MatsimTestUtils.EPSILON );
	}

	@Test
	void test_whenModifyingTypeSmall_itModifiesCostInfoCorrectly(){
		VehicleType small = types.getVehicleTypes().get(Id.create("small", org.matsim.vehicles.VehicleType.class ) );
		Assertions.assertEquals(25.0, small.getCostInformation().getFixedCosts(),0.01 );
		Assertions.assertEquals(0.75, small.getCostInformation().getCostsPerMeter(),0.01 );
		Assertions.assertEquals(0.25, small.getCostInformation().getCostsPerSecond(),0.01 );
	}

	@Test
	void test_whenModifyingTypeSmall_itModifiesEngineInfoCorrectly(){
		VehicleType small = types.getVehicleTypes().get(Id.create("small", org.matsim.vehicles.VehicleType.class ) );
		Assertions.assertEquals(0.015, small.getEngineInformation().getFuelConsumption(),0.001);
		Assertions.assertEquals(FuelType.gasoline, small.getEngineInformation().getFuelType());
	}

	@Test
	void test_whenModifyingTypeSmall_itModifiesMaxVelocityCorrectly(){
		VehicleType small = types.getVehicleTypes().get(Id.create("small", org.matsim.vehicles.VehicleType.class ) );
		Assertions.assertEquals(10.0, small.getMaximumVelocity(), 0.01);
	}


}
