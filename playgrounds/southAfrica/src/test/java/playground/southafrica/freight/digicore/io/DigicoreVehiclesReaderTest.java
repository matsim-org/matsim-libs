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
import playground.southafrica.freight.digicore.containers.DigicorePosition;
import playground.southafrica.freight.digicore.containers.DigicoreTrace;
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
		
		/* V1 */
		new DigicoreVehiclesWriter(dvs).writeV1(utils.getOutputDirectory() + "vehiclesV1.xml.gz");
		assertTrue("Output file does not exist", new File(utils.getOutputDirectory() + "vehiclesV1.xml.gz").exists());
		
		/* V2 */
		new DigicoreVehiclesWriter(dvs).writeV2(utils.getOutputDirectory() + "vehiclesV2.xml.gz");
		assertTrue("Output file does not exist", new File(utils.getOutputDirectory() + "vehiclesV2.xml.gz").exists());
	}
	
	@Test
	public void testVehiclesReaderV1(){
		DigicoreVehicles dvsOut = createVehicles();
		new DigicoreVehiclesWriter(dvsOut).writeV1(utils.getOutputDirectory() + "vehiclesV1.xml");
		
		DigicoreVehicles dvsIn = createVehicles();
		new DigicoreVehiclesReader(dvsIn).readFile(utils.getOutputDirectory() + "vehiclesV1.xml");
		/* Check container. */
		assertTrue("Wrong CRS.", dvsIn.getCoordinateReferenceSystem().equalsIgnoreCase(dvsOut.getCoordinateReferenceSystem()));
		assertTrue("Wrong description.", dvsIn.getDescription().equalsIgnoreCase(dvsOut.getDescription()));
		/* Check vehicles. */
		assertTrue("Wrong number of vehicles.", dvsIn.getVehicles().size() == 2);
		assertNotNull("Cannot find vehicle '1'.", dvsIn.getVehicles().get(Id.createVehicleId("1")));
		assertNotNull("Cannot find vehicle '2'.", dvsIn.getVehicles().get(Id.createVehicleId("2")));
		/* No need to test any further... I hope. */
	}
	
	@Test
	public void testVehiclesReaderV2(){
		DigicoreVehicles dvsOut = createVehicles();
		new DigicoreVehiclesWriter(dvsOut).writeV2(utils.getOutputDirectory() + "vehiclesV2.xml");
		
		DigicoreVehicles dvsIn = createVehicles();
		new DigicoreVehiclesReader(dvsIn).readFile(utils.getOutputDirectory() + "vehiclesV2.xml");
		/* Check container. */
		assertTrue("Wrong CRS.", dvsIn.getCoordinateReferenceSystem().equalsIgnoreCase(dvsOut.getCoordinateReferenceSystem()));
		assertTrue("Wrong description.", dvsIn.getDescription().equalsIgnoreCase(dvsOut.getDescription()));
		/* Check vehicles. */
		assertTrue("Wrong number of vehicles.", dvsIn.getVehicles().size() == 2);
		assertNotNull("Cannot find vehicle '1'.", dvsIn.getVehicles().get(Id.createVehicleId("1")));
		assertNotNull("Cannot find vehicle '2'.", dvsIn.getVehicles().get(Id.createVehicleId("2")));
		/* No need to test any further... I hope. */
		
		/* Check chain of the second vehicle. */
		DigicoreChain chain = dvsIn.getVehicles().get(Id.createVehicleId("2")).getChains().get(0);
		assertTrue("Chain should be complete.", chain.isComplete());
		assertEquals("Wrong number of chain elements.", 5, chain.size());
		assertTrue("Wrong element type.", chain.get(0) instanceof DigicoreActivity);
		assertTrue("Wrong element type.", chain.get(1) instanceof DigicoreTrace);
		assertTrue("Wrong element type.", chain.get(2) instanceof DigicoreActivity);
		assertTrue("Wrong element type.", chain.get(3) instanceof DigicoreTrace);
		assertTrue("Wrong element type.", chain.get(4) instanceof DigicoreActivity);
		
		/* Check traces of the second vehicle. */
		DigicoreTrace trace = (DigicoreTrace) chain.get(1);
		assertEquals("Wrong number of positions.", 4, trace.size());
		assertTrue("Wrong CRS for trace.", dvsIn.getCoordinateReferenceSystem().equalsIgnoreCase(trace.getCrs()));
		DigicorePosition position = trace.get(0);
		assertEquals("Position time not correct.", position.getTimeAsGregorianCalendar().getTimeInMillis(), 6000l);
		assertEquals("Position x not correct.", position.getCoord().getX(), 1.0, MatsimTestUtils.EPSILON);
		assertEquals("Position y not correct.", position.getCoord().getY(), 1.0, MatsimTestUtils.EPSILON);
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
		
		DigicoreTrace d1c1t1 = new DigicoreTrace(dvs.getCoordinateReferenceSystem());
		d1c1t1.add(new DigicorePosition(6, 1.0, 1.0));
		d1c1t1.add(new DigicorePosition(7, 2.0, 2.0));
		d1c1t1.add(new DigicorePosition(8, 3.0, 3.0));
		d1c1t1.add(new DigicorePosition(9, 4.0, 4.0));
		d1c1.add(d1c1t1);
		
		DigicoreActivity d1c1a2 = new DigicoreActivity("minor", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		d1c1a2.setCoord(new Coord((double) 5, (double) 5));
		d1c1a2.setStartTime(10);
		d1c1a2.setEndTime(15);
		d1c1.add(d1c1a2);

		DigicoreTrace d1c1t2 = new DigicoreTrace(dvs.getCoordinateReferenceSystem());
		d1c1t2.add(new DigicorePosition(16, 6.0, 6.0));
		d1c1t2.add(new DigicorePosition(17, 7.0, 7.0));
		d1c1t2.add(new DigicorePosition(18, 8.0, 8.0));
		d1c1t2.add(new DigicorePosition(19, 9.0, 9.0));
		d1c1.add(d1c1t2);
		
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
		
		DigicoreTrace d2c1t1 = new DigicoreTrace(dvs.getCoordinateReferenceSystem());
		d2c1t1.add(new DigicorePosition(6, 1.0, 1.0));
		d2c1t1.add(new DigicorePosition(7, 2.0, 2.0));
		d2c1t1.add(new DigicorePosition(8, 3.0, 3.0));
		d2c1t1.add(new DigicorePosition(9, 4.0, 4.0));
		d2c1.add(d2c1t1);
		
		DigicoreActivity d2c1a2 = new DigicoreActivity("minor", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		d2c1a2.setCoord(new Coord((double) 5, (double) 5));
		d2c1a2.setStartTime(10);
		d2c1a2.setEndTime(15);
		d2c1.add(d2c1a2);

		DigicoreTrace d2c1t2 = new DigicoreTrace(dvs.getCoordinateReferenceSystem());
		d2c1t2.add(new DigicorePosition(16, 6.0, 6.0));
		d2c1t2.add(new DigicorePosition(17, 7.0, 7.0));
		d2c1t2.add(new DigicorePosition(18, 8.0, 8.0));
		d2c1t2.add(new DigicorePosition(19, 9.0, 9.0));
		d2c1.add(d2c1t2);
		
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

