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

import org.junit.Rule;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.misc.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;

public class DigicoreVehicleWriterTest{
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	public void testWriteVehicle(){
		DigicoreVehicle v = createVehicle();
		DigicoreVehicleWriter dvw = new DigicoreVehicleWriter();
		dvw.write(utils.getOutputDirectory() + "tmp.xml", v);
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

