/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,     *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.vehicles;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;

/**
 *
 * @author dgrether
 * @author jwjoubert
 */
public class VehiclesImplTest {

	@Test
	public void testAddVehicle() {
		Vehicles vehicles = VehicleUtils.createVehiclesContainer();

		VehicleType testType = vehicles.getFactory().createVehicleType(Id.create("test", VehicleType.class));
		Vehicle v1 = vehicles.getFactory().createVehicle(Id.create("v1", Vehicle.class), testType);

		/* Must add vehicle type before adding vehicle. */
		try{
			vehicles.addVehicle(v1);
			Assert.fail("Should not allow adding a vehicle if vehicle type has not been added to container first.");
		} catch(IllegalArgumentException e){
			/* Pass. */
		}

		vehicles.addVehicleType(testType);
		vehicles.addVehicle(v1);

		Vehicle v2 = vehicles.getFactory().createVehicle(Id.create("v1", Vehicle.class), testType);
		try{
			vehicles.addVehicle(v2);
			Assert.fail("Cannot add another vehicle with the same Id.");
		} catch (IllegalArgumentException e){
			/* Pass. */
		}
	}


	@Test
	public void testGetVehicles(){
		Vehicles vehicles = VehicleUtils.createVehiclesContainer();

		VehicleType testType = vehicles.getFactory().createVehicleType(Id.create("test", VehicleType.class));
		vehicles.addVehicleType(testType);
		Vehicle v1 = vehicles.getFactory().createVehicle(Id.create("v1", Vehicle.class), testType);

		/* Should get an unmodifiable Map of the Vehicles container. */
		try{
			vehicles.getVehicles().put(Id.create("v1", Vehicle.class), v1 );
			vehicles.getVehicles();
			Assert.fail("Should not be able to add to an unmodiafiable Map");
		} catch (UnsupportedOperationException e){
			/* Pass. */
		}
	}


	@Test
	public void testGetVehicleTypes(){
		Vehicles vehicles = VehicleUtils.createVehiclesContainer();

		VehicleType t1 = vehicles.getFactory().createVehicleType(Id.create("type1", VehicleType.class));
		/* Should get an unmodifiable Map of the Vehicles container. */
		try{
			vehicles.getVehicleTypes().put(Id.create("type1", VehicleType.class), t1 );
			Assert.fail("Should not be able to add to an unmodiafiable Map");
		} catch (UnsupportedOperationException e){
			/* Pass. */
		}
	}

	@Test
	public void testAddVehicleType(){
		Vehicles vehicles = VehicleUtils.createVehiclesContainer();

		VehicleType t1 = vehicles.getFactory().createVehicleType(Id.create("type1", VehicleType.class));
		VehicleType t2 = vehicles.getFactory().createVehicleType(Id.create("type1", VehicleType.class));

		vehicles.addVehicleType(t1);
		try{
			vehicles.addVehicleType(t2);
			Assert.fail("Cannot add another vehicle type with the same Id");
		} catch (IllegalArgumentException e){
			/* Pass. */
		}
	}

	@Test
	public void testRemoveVehicle() {
		Vehicles vehicles = VehicleUtils.createVehiclesContainer();
		VehicleType t1 = vehicles.getFactory().createVehicleType(Id.create("type1", VehicleType.class));
		vehicles.addVehicleType(t1);

		Vehicle v1 = vehicles.getFactory().createVehicle(Id.create("v1", Vehicle.class), t1);

		Assert.assertEquals(0, vehicles.getVehicles().size());
		vehicles.addVehicle(v1);
		Assert.assertEquals(1, vehicles.getVehicles().size());
		vehicles.removeVehicle(Id.create("v1", Vehicle.class));
		Assert.assertEquals(0, vehicles.getVehicles().size());
	}

	@Test
	public void testRemoveVehicleType() {
		Vehicles vehicles = VehicleUtils.createVehiclesContainer();
		VehicleType t1 = vehicles.getFactory().createVehicleType(Id.create("type1", VehicleType.class));
		vehicles.addVehicleType(t1);
		Vehicle v1 = vehicles.getFactory().createVehicle(Id.create("v1", Vehicle.class), t1);
		vehicles.addVehicle(v1);

		try {
			vehicles.removeVehicleType(t1.getId());
			Assert.fail("expected exception, as vehicle type is still in use.");
		} catch (IllegalArgumentException e) {
			// pass
		}

		vehicles.removeVehicle(v1.getId());
		vehicles.removeVehicleType(t1.getId());

		// also test for non-existant vehicle types
		vehicles.removeVehicleType(Id.create("type2", VehicleType.class));
	}

}
