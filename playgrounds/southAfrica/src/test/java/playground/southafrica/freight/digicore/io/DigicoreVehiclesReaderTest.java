/* *********************************************************************** *
 * project: org.matsim.*
 * DigicoreVehiclesReader.java                                                                        *
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
package playground.southafrica.freight.digicore.io;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.freight.digicore.containers.DigicoreVehicles;

public class DigicoreVehiclesReaderTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testCreateVehicles() {
		DigicoreVehicles dvs = createVehicles();
		assertEquals("Wrong number of vehicles", 2, dvs.getVehicles().size());
	}
	
	@Test
	/**
	 * This is already tested in another class, but rather safe than sorry.
	 */
	public void testVehiclesWriter(){
		DigicoreVehicles dvs = createVehicles();
		new DigicoreVehiclesWriter(dvs).write(utils.getOutputDirectory() + "vehicles.xml");
		assertTrue("Output file does not exist", new File(utils.getOutputDirectory() + "vehicles.xml").exists());
	}
	
	@Test
	public void testVehiclesReader(){
		DigicoreVehicles dvsOut = createVehicles();
		new DigicoreVehiclesWriter(dvsOut).write(utils.getOutputDirectory() + "vehicles.xml");
		
		DigicoreVehicles dvsIn = createVehicles();
		new DigicoreVehiclesReader(dvsIn).parse(utils.getOutputDirectory() + "vehicles.xml");
		/* Check container. */
		assertTrue("Wrong CRS.", dvsIn.getCoordinateReferenceSystem().equalsIgnoreCase(dvsOut.getCoordinateReferenceSystem()));
		assertTrue("Wrong description.", dvsIn.getDescription().equalsIgnoreCase(dvsOut.getDescription()));
		/* Check vehicles. */
		assertTrue("Wrong number of vehicles.", dvsIn.getVehicles().size() == 2);
		assertNotNull("Cannot find vehicle '1'.", dvsIn.getVehicles().get(Id.createVehicleId("1")));
		assertNotNull("Cannot find vehicle '2'.", dvsIn.getVehicles().get(Id.createVehicleId("2")));
		/* No need to test any further... I hope. */
	}
	
	private DigicoreVehicles createVehicles(){
		DigicoreVehicles dvs = new DigicoreVehicles();
		dvs.setCoordinateReferenceSystem("Test CRS");
		dvs.setDescription("Test");
		
		/* Vehicle 1. */
		DigicoreVehicle veh1 = new DigicoreVehicle(Id.create("1", Vehicle.class));
		DigicoreChain d1c1 = new DigicoreChain();

		DigicoreActivity d1c1a1 = new DigicoreActivity("major", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		d1c1a1.setCoord(new Coord((double) 0, (double) 0));
		d1c1a1.setStartTime(0);
		d1c1a1.setEndTime(5);
		d1c1.add(d1c1a1);
		
		DigicoreActivity d1c1a2 = new DigicoreActivity("minor", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		d1c1a2.setCoord(new Coord((double) 5, (double) 5));
		d1c1a2.setStartTime(10);
		d1c1a2.setEndTime(15);
		d1c1.add(d1c1a2);
		
		DigicoreActivity d1c1a3 = new DigicoreActivity("major", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		d1c1a3.setCoord(new Coord((double) 10, (double) 10));
		d1c1a3.setStartTime(20);
		d1c1a3.setEndTime(25);
		d1c1.add(d1c1a3);
		
		veh1.getChains().add(d1c1);
		dvs.addDigicoreVehicle(veh1);
		
		/* Vehicle 2. */
		DigicoreVehicle veh2 = new DigicoreVehicle(Id.create("2", Vehicle.class));
		DigicoreChain d2c1 = new DigicoreChain();
		
		DigicoreActivity d2c1a1 = new DigicoreActivity("major", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		d2c1a1.setCoord(new Coord((double) 0, (double) 0));
		d2c1a1.setStartTime(0);
		d2c1a1.setEndTime(5);
		d2c1.add(d2c1a1);
		
		DigicoreActivity d2c1a2 = new DigicoreActivity("minor", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		d2c1a2.setCoord(new Coord((double) 5, (double) 5));
		d2c1a2.setStartTime(10);
		d2c1a2.setEndTime(15);
		d2c1.add(d2c1a2);
		
		DigicoreActivity d2c1a3 = new DigicoreActivity("major", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		d2c1a3.setCoord(new Coord((double) 10, (double) 10));
		d2c1a3.setStartTime(20);
		d2c1a3.setEndTime(25);
		d2c1.add(d2c1a3);
		
		veh2.getChains().add(d2c1);
		
		dvs.addDigicoreVehicle(veh2);
		return dvs;
	}


}
