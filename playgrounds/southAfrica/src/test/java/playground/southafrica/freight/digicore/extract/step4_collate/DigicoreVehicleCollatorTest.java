/* *********************************************************************** *
 * project: org.matsim.*
 * DigicoreVehicleCollatorTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.southafrica.freight.digicore.extract.step4_collate;

import java.io.File;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.testcases.MatsimTestUtils;

import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.freight.digicore.io.DigicoreVehicleWriter;

public class DigicoreVehicleCollatorTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testBuild(){
		buildTestCase();
		Assert.assertTrue("Cannot find XML folder.", new File(utils.getOutputDirectory() + "xml/").exists());
		Assert.assertTrue("Cannot find first vehicle.", new File(utils.getOutputDirectory() + "xml/1.xml.gz").exists());
		Assert.assertTrue("Cannot find second.", new File(utils.getOutputDirectory() + "xml/2.xml.gz").exists());
	}
	
	@Test 
	public void testCollateWithoutDelete(){
		buildTestCase();
		
		/* Test without deleting the input folder. */
		String folder = utils.getOutputDirectory() + "xml/";
		String filename = utils.getOutputDirectory() + "output.xml";
		String[] args = {folder, filename, "Atlantis", "false"};
		DigicoreVehicleCollator.main(args);
		Assert.assertTrue("XML folder should exist.", new File(utils.getOutputDirectory() + "xml/").exists());
		Assert.assertTrue("First vehicle should still exist.", new File(utils.getOutputDirectory() + "xml/1.xml.gz").exists());
		Assert.assertTrue("Second vehicle should still exist.", new File(utils.getOutputDirectory() + "xml/2.xml.gz").exists());
		Assert.assertTrue("Output file should exist.", new File(utils.getOutputDirectory() + "output.xml").exists());
	}
	
	
	@Test 
	public void testCollateWithDelete(){
		buildTestCase();
		
		/* Test with deleting the input folder. */
		String folder = utils.getOutputDirectory() + "xml/";
		String filename = utils.getOutputDirectory() + "output.xml";
		String[] args = {folder, filename, "Atlantis", "true"};
		DigicoreVehicleCollator.main(args);
		Assert.assertFalse("XML folder should NOT exist.", new File(utils.getOutputDirectory() + "xml/").exists());
		Assert.assertTrue("Output file should exist.", new File(utils.getOutputDirectory() + "output.xml").exists());
	}
	
	
	/**
	 * Create two basic {@link DigicoreVehicle}s and write to a folder.
	 */
	private void buildTestCase(){
		/* Create the xml folder. */
		String folder = utils.getOutputDirectory() + "xml/";
		new File(folder).mkdirs();
		
		/* Create general chain. */
		Coord coord = CoordUtils.createCoord(0.0, 0.0);
		DigicoreActivity da = new DigicoreActivity("test", TimeZone.getTimeZone("GMT+2"), Locale.ENGLISH);
		da.setCoord(coord);
		DigicoreChain chain = new DigicoreChain();
		chain.add(da);
		
		/* Vehicle 1 */
		DigicoreVehicle dv1 = new DigicoreVehicle(Id.createVehicleId("1"));
		dv1.getChains().add(chain);
		new DigicoreVehicleWriter().write(folder + "1.xml.gz", dv1);
		
		/* Vehicle 2 */
		DigicoreVehicle dv2 = new DigicoreVehicle(Id.createVehicleId("2"));
		dv2.getChains().add(chain);
		new DigicoreVehicleWriter().write(folder + "2.xml.gz", dv2);
	}
}
