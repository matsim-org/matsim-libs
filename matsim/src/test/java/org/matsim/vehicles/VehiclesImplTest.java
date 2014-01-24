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
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class VehiclesImplTest {

	@Test
	public void testGetVehicles(){
		Vehicles vehicles = VehicleUtils.createVehiclesContainer();
		
		Vehicle v1 = vehicles.getFactory().createVehicle(new IdImpl("v1"), new VehicleTypeImpl(new IdImpl("test")));

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
	public void testAddVehicle() {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		Vehicles vehicles = VehicleUtils.createVehiclesContainer();
		
		Vehicle v1 = vehicles.getFactory().createVehicle(new IdImpl("v1"), new VehicleTypeImpl(new IdImpl("test")));
		Vehicle v2 = vehicles.getFactory().createVehicle(new IdImpl("v1"), new VehicleTypeImpl(new IdImpl("test")));
		
		vehicles.addVehicle(v1);
		try{
			vehicles.addVehicle(v2);
			Assert.fail("Cannot add another vehicle with the same Id.");
		} catch (IllegalArgumentException e){
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
	

}
