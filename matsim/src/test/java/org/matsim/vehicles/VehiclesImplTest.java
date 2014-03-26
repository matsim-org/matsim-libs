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
import org.matsim.core.basic.v01.IdImpl;

/**
 * 
 * @author dgrether
 * @author jwjoubert
 */
public class VehiclesImplTest {

	@Test
	public void testAddVehicle() {
		Vehicles vehicles = VehicleUtils.createVehiclesContainer();
		
		VehicleType testType = vehicles.getFactory().createVehicleType(new IdImpl("test"));
		Vehicle v1 = vehicles.getFactory().createVehicle(new IdImpl("v1"), testType);

		/* Must add vehicle type before adding vehicle. */
		try{
			vehicles.addVehicle(v1);
			Assert.fail("Should not allow adding a vehicle if vehicle type has not been added to container first.");
		} catch(IllegalArgumentException e){
			/* Pass. */
		}
		
		vehicles.addVehicleType(testType);
		vehicles.addVehicle(v1);
		
		Vehicle v2 = vehicles.getFactory().createVehicle(new IdImpl("v1"), testType);
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

		VehicleType testType = vehicles.getFactory().createVehicleType(new IdImpl("test"));
		vehicles.addVehicleType(testType);
		Vehicle v1 = vehicles.getFactory().createVehicle(new IdImpl("v1"), testType);

		/* Should get an unmodifiable Map of the Vehicles container. */
		try{
			vehicles.getVehicles().put(new IdImpl("v1"), v1 );
			vehicles.getVehicles();
			Assert.fail("Should not be able to add to an unmodiafiable Map");
		} catch (UnsupportedOperationException e){
			/* Pass. */
		}
	}

	
	@Test
	public void testGetVehicleTypes(){
		Vehicles vehicles = VehicleUtils.createVehiclesContainer();

		VehicleType t1 = vehicles.getFactory().createVehicleType(new IdImpl("type1"));
		/* Should get an unmodifiable Map of the Vehicles container. */
		try{
			vehicles.getVehicleTypes().put(new IdImpl("type1"), t1 );
			Assert.fail("Should not be able to add to an unmodiafiable Map");
		} catch (UnsupportedOperationException e){
			/* Pass. */
		}
	}

	@Test
	public void testAddVehicleType(){
		Vehicles vehicles = VehicleUtils.createVehiclesContainer();

		VehicleType t1 = vehicles.getFactory().createVehicleType(new IdImpl("type1"));
		VehicleType t2 = vehicles.getFactory().createVehicleType(new IdImpl("type1"));

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
		VehicleType t1 = vehicles.getFactory().createVehicleType(new IdImpl("type1"));
		vehicles.addVehicleType(t1);

		Vehicle v1 = vehicles.getFactory().createVehicle(new IdImpl("v1"), t1);

		Assert.assertEquals(0, vehicles.getVehicles().size());
		vehicles.addVehicle(v1);
		Assert.assertEquals(1, vehicles.getVehicles().size());
		vehicles.removeVehicle(new IdImpl("v1"));
		Assert.assertEquals(0, vehicles.getVehicles().size());
	}

}
