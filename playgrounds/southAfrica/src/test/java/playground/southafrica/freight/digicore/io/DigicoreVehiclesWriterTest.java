/* *********************************************************************** *
 * project: org.matsim.*
 * DigicoreVehiclesWriterTest.java                                                                        *
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
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.testcases.MatsimTestUtils;

import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.freight.digicore.containers.DigicoreVehicles;

public class DigicoreVehiclesWriterTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testBuildVehicles(){
		DigicoreVehicles dvs = buildVehicles();
		assertTrue("Wrong number of vehicles.", dvs.getVehicles().size() == 2);
	}
	
	@Test
	public void testWriteV1() {
		DigicoreVehicles dvs = buildVehicles();
		new DigicoreVehiclesWriter(dvs).writeV1(utils.getOutputDirectory() + "vehicles.xml");
		assertTrue("Output file does not exist.", new File(utils.getOutputDirectory() + "vehicles.xml").exists());
	}
	
	private DigicoreVehicles buildVehicles(){
		DigicoreVehicles dvs = new DigicoreVehicles();
		dvs.setCoordinateReferenceSystem("Test CRS");
		dvs.setDescription("Test");
		
		/* Empty vehicle. */
		DigicoreVehicle dv1 = new DigicoreVehicle(Id.createVehicleId("1"));
		dvs.addDigicoreVehicle(dv1);
		
		/* Vehicle with one chain. */
		DigicoreVehicle dv2 = new DigicoreVehicle(Id.createVehicleId("2"));
		DigicoreChain chain = new DigicoreChain();
		
		DigicoreActivity act = new DigicoreActivity("TestActivity", TimeZone.getTimeZone("GMT+2"), Locale.ENGLISH);
		act.setCoord(CoordUtils.createCoord(0.0, 0.0));
		chain.add(act);
		dv2.getChains().add(chain);
		dvs.addDigicoreVehicle(dv2);
		
		return dvs;
	}

}
