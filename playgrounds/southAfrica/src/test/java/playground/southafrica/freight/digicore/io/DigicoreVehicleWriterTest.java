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

public class DigicoreVehicleWriterTest{
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testWriteV1Vehicle(){
		DigicoreVehicle v = createV1Vehicle();
		DigicoreVehicleWriter dvw = new DigicoreVehicleWriter(v);
		try{
			dvw.writeV1(utils.getOutputDirectory() + "tmp.xml");
		} catch(Exception e){
			fail("Should write vehicle without exception.");
		}
		assertTrue("File should exist.", new File(utils.getOutputDirectory() + "tmp.xml").exists());
	}

	@Test
	public void testWriteV2Vehicle(){
		DigicoreVehicle v = createV2Vehicle();
		DigicoreVehicleWriter dvw = new DigicoreVehicleWriter(v);
		try{
			dvw.writeV2(utils.getOutputDirectory() + "tmp.xml");
		} catch(Exception e){
			fail("Should write vehicle without exception.");
		}
		assertTrue("File should exist.", new File(utils.getOutputDirectory() + "tmp.xml").exists());
	}
	
	
	private DigicoreVehicle createV1Vehicle(){
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

	private DigicoreVehicle createV2Vehicle(){
		DigicoreVehicle vehicle = new DigicoreVehicle(Id.create("1", Vehicle.class));
		
		DigicoreChain dc = new DigicoreChain();
		
		DigicoreActivity da1 = new DigicoreActivity("major", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		da1.setCoord(new Coord((double) 0, (double) 0));
		da1.setStartTime(0);
		da1.setEndTime(5);
		dc.add(da1);
		
		DigicoreTrace t1 = new DigicoreTrace("Atlantis");
		t1.add(new DigicorePosition(6, 1.0, 1.0));
		t1.add(new DigicorePosition(7, 2.0, 2.0));
		t1.add(new DigicorePosition(8, 3.0, 3.0));
		t1.add(new DigicorePosition(9, 4.0, 4.0));
		t1.add(new DigicorePosition(10, 5.0, 5.0));
		dc.add(t1);
		
		DigicoreActivity da2 = new DigicoreActivity("minor", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		da2.setCoord(new Coord((double) 5, (double) 5));
		da2.setStartTime(10);
		da2.setEndTime(15);
		dc.add(da2);

		DigicoreTrace t2 = new DigicoreTrace("Atlantis");
		t2.add(new DigicorePosition(16, 6.0, 6.0));
		t2.add(new DigicorePosition(17, 7.0, 7.0));
		t2.add(new DigicorePosition(18, 8.0, 8.0));
		t2.add(new DigicorePosition(19, 9.0, 9.0));
		t2.add(new DigicorePosition(20, 10.0, 10.0));
		dc.add(t2);
		
		DigicoreActivity da3 = new DigicoreActivity("major", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		da3.setCoord(new Coord((double) 10, (double) 10));
		da3.setStartTime(20);
		da3.setEndTime(25);
		dc.add(da3);
		
		vehicle.getChains().add(dc);
		return vehicle;
	}
	
	
}

