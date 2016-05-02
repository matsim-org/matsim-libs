/* *********************************************************************** *
 * project: org.matsim.*
 * DigicoreVehiclesTest.java                                                                        *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
package playground.southafrica.freight.digicore.containers;

import static org.junit.Assert.*;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.testcases.MatsimTestUtils;

public class DigicoreVehiclesTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testDescription() {
		DigicoreVehicles dv = new DigicoreVehicles();
		assertNull("Description should be NULL", dv.getDescription());
		String descr = "Test";
		dv.setDescription(descr);
		assertTrue("Wrong descripotion", descr.equalsIgnoreCase(dv.getDescription()));
	}

	@Test
	public void testAddDigicoreVehicle() {
		DigicoreVehicles dv = new DigicoreVehicles();
		
		DigicoreVehicle v1 = new DigicoreVehicle(Id.createVehicleId("1"));
		dv.addDigicoreVehicle(v1);
		assertEquals("Wrong number of vehicles.", 1, dv.getVehicles().size());
		assertNotNull("Cannot find vehicle '1'", dv.getVehicles().get(Id.createVehicleId("1")));
		
		try{
			dv.addDigicoreVehicle(v1);
			fail("Should not accept a vehicle with the same Id.");
		} catch (IllegalArgumentException e){
			/* Pass. */
		}
		
		DigicoreVehicle v2 = new DigicoreVehicle(Id.createVehicleId("2"));
		dv.addDigicoreVehicle(v2);
		assertEquals("Wrong number of vehicles.", 2, dv.getVehicles().size());
		assertNotNull("Cannot find vehicle '2'", dv.getVehicles().get(Id.createVehicleId("2")));
	}
	
	@Test
	public void testSilentLog(){
		DigicoreVehicles dv = new DigicoreVehicles();
		assertFalse("Logs should not be silent.", dv.isSilent());
		
		dv.setSilentLog(true);
		assertTrue("Logs should be silent.", dv.isSilent());
		
		dv.setSilentLog(false);
		assertFalse("Logs should not be silent.", dv.isSilent());
	}
	
	@Test
	public void testCRS(){
		DigicoreVehicles dv1 = new DigicoreVehicles();
		assertTrue("Wrong CRS.", dv1.getCoordinateReferenceSystem().equalsIgnoreCase("Atlantis"));
		
		String crs = "Test";
		dv1.setCoordinateReferenceSystem(crs);
		assertTrue("Wrong CRS.", dv1.getCoordinateReferenceSystem().equalsIgnoreCase(crs));
		
		DigicoreVehicles dv2 = new DigicoreVehicles(crs);
		assertTrue("Wrong CRS.", dv2.getCoordinateReferenceSystem().equalsIgnoreCase(crs));
	}

}
