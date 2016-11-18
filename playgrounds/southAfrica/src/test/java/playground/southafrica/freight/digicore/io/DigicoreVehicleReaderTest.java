/* *********************************************************************** *
 * project: org.matsim.*
 * SouthAfricanInflationCorrectorTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
import playground.southafrica.freight.digicore.containers.DigicoreTrace;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;

public class DigicoreVehicleReaderTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testReadVehicleV1(){
		DigicoreVehicle v1 = createVehicle();
		DigicoreVehicleWriter dvw = new DigicoreVehicleWriter(v1);
		try{
			dvw.writeV1(utils.getOutputDirectory() + "tmp.xml");
		} catch(Exception e){
			fail("Should write without exception.");
		}
		
		DigicoreVehicleReader_v1 dvr = new DigicoreVehicleReader_v1();
		dvr.readFile(utils.getOutputDirectory() + "tmp.xml");
		DigicoreVehicle v2 = dvr.getVehicle();
		
		assertEquals("Wrong id.", true, v1.getId().toString().equalsIgnoreCase(v2.getId().toString()));
		assertEquals("Wrong number of chains.", 1, v2.getChains().size());
		assertEquals("Wrong number of activities in chain.", 3, v2.getChains().get(0).getAllActivities().size());

		assertTrue("Wrong activity type.", "minor".equalsIgnoreCase(v2.getChains().get(0).getAllActivities().get(1).getType()));
		assertTrue("Wrong coordinate.", v2.getChains().get(0).getAllActivities().get(1).getCoord().equals(new Coord((double) 5, (double) 5)));
		assertEquals("Wrong start time.", 
				v1.getChains().get(0).getAllActivities().get(1).getStartTime(), 
				v2.getChains().get(0).getAllActivities().get(1).getStartTime(),
				MatsimTestUtils.EPSILON);
		assertEquals("Wrong end time.", 
				v1.getChains().get(0).getAllActivities().get(1).getEndTime(), 
				v2.getChains().get(0).getAllActivities().get(1).getEndTime(),
				MatsimTestUtils.EPSILON);
	}
	
	@Test
	public void testReadVehicleV2(){
		DigicoreVehicleReader dvr = new DigicoreVehicleReader();
		try{
			dvr.readFile(utils.getClassInputDirectory() + "testV2.xml.gz");
		} catch (Exception e){
			e.printStackTrace();
			fail("Should read without exception.");
		}
		DigicoreVehicle vehicle = dvr.getVehicle();
		
		assertTrue("Wrong id", vehicle.getId().toString().equalsIgnoreCase("1"));
		assertEquals("Wrong number of chains", 1, vehicle.getChains().size());
		DigicoreChain chain = vehicle.getChains().get(0);
		assertEquals("Wrong number of chain elements.", 5, chain.size());
		assertTrue("Chain should be complete.", chain.isComplete());
		
		/* Check chain element types. */
		assertTrue("First element should be activity.", chain.get(0) instanceof DigicoreActivity);
		assertTrue("Second element should be trace.", chain.get(1) instanceof DigicoreTrace);
		assertTrue("Third element should be activity.", chain.get(2) instanceof DigicoreActivity);
		assertTrue("Fourth element should be trace.", chain.get(3) instanceof DigicoreTrace);
		assertTrue("Fifth element should be activity.", chain.get(4) instanceof DigicoreActivity);

		/* Check that all positions are read. */
		assertEquals("Wrong number of positions.", 5, ((DigicoreTrace) chain.get(1)).size());
		assertEquals("Wrong number of positions.", 5, ((DigicoreTrace) chain.get(3)).size());
	}

	
	private DigicoreVehicle createVehicle(){
		DigicoreVehicle vehicle = new DigicoreVehicle(Id.create("1", Vehicle.class));
		
		DigicoreChain dc = new DigicoreChain();
		
		DigicoreActivity da1 = new DigicoreActivity("major", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		da1.setCoord(new Coord((double) 0, (double) 0));
		da1.setStartTime(0);
		da1.setEndTime(5);
		dc.add(da1);
		
		DigicoreActivity da2 = new DigicoreActivity("minor", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		da2.setCoord(new Coord((double) 5, (double) 5));
		da2.setStartTime(10);
		da2.setEndTime(15);
		dc.add(da2);
		
		DigicoreActivity da3 = new DigicoreActivity("major", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		da3.setCoord(new Coord((double) 10, (double) 10));
		da3.setStartTime(20);
		da3.setEndTime(25);
		dc.add(da3);
		
		vehicle.getChains().add(dc);
		return vehicle;
	}

	
}

