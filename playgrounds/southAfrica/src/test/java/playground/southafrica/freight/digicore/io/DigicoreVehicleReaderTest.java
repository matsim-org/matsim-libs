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


import java.util.Locale;
import java.util.TimeZone;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;

public class DigicoreVehicleReaderTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testReadVehicle(){
		DigicoreVehicle v1 = createVehicle();
		DigicoreVehicleWriter dvw = new DigicoreVehicleWriter();
		try{
			dvw.write(utils.getOutputDirectory() + "tmp.xml");
			Assert.fail();
		} catch(IllegalArgumentException e){
			/* Pass. */
		}
		dvw.write(utils.getOutputDirectory() + "tmp.xml", v1);
		
		DigicoreVehicleReader_v1 dvr = new DigicoreVehicleReader_v1();
		dvr.parse(utils.getOutputDirectory() + "tmp.xml");
		DigicoreVehicle v2 = dvr.getVehicle();
		
		Assert.assertEquals("Wrong id.", true, v1.getId().toString().equalsIgnoreCase(v2.getId().toString()));
		Assert.assertEquals("Wrong number of chains.", 1, v2.getChains().size());
		Assert.assertEquals("Wrong number of activities in chain.", 3, v2.getChains().get(0).getAllActivities().size());

		Assert.assertTrue("Wrong activity type.", "minor".equalsIgnoreCase(v2.getChains().get(0).getAllActivities().get(1).getType()));
		Assert.assertTrue("Wrong coordinate.", v2.getChains().get(0).getAllActivities().get(1).getCoord().equals(new Coord((double) 5, (double) 5)));
		Assert.assertEquals("Wrong start time.", 
				v1.getChains().get(0).getAllActivities().get(1).getStartTime(), 
				v2.getChains().get(0).getAllActivities().get(1).getStartTime(),
				MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong end time.", 
				v1.getChains().get(0).getAllActivities().get(1).getEndTime(), 
				v2.getChains().get(0).getAllActivities().get(1).getEndTime(),
				MatsimTestUtils.EPSILON);
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

