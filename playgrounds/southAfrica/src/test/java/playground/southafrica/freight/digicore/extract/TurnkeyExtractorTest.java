/* *********************************************************************** *
 * project: org.matsim.*
 * TurnkeyExtractorTest.java
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

package playground.southafrica.freight.digicore.extract;

import java.io.File;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.freight.digicore.containers.DigicoreVehicles;
import playground.southafrica.freight.digicore.io.DigicoreVehiclesReader;

/**
 * Integration test to ensure the entire activity chain extraction remains
 * consistent and 'correct' (Can I say that with certainty?!).
 * 
 * @author jwjoubert
 */
public class TurnkeyExtractorTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testInputFile(){
		File file = new File(utils.getClassInputDirectory() + "/test.csv.gz");
		if(!file.exists()){
			Assert.fail("Input file doesn't exist: " + file.getAbsolutePath());
		}
	}
	
	@Test
	public void testStatusFile(){
		File file = new File(utils.getClassInputDirectory() + "/status.csv");
		if(!file.exists()){
			Assert.fail("Input file doesn't exist: " + file.getAbsolutePath());
		}
	}
	
	@Test
	public void testIntegrationExtraction(){
		
		String[] args = getTestArguments();
		try{
			TurnkeyExtractor.main(args);
		} catch(Exception e){
			e.printStackTrace();
			Assert.fail("Should extract without exceptions.");
		}
		
		/* Check that the output files exist. */
		File f1 = new File(utils.getOutputDirectory() + "Vehicles/10001.txt.gz");
		Assert.assertTrue("Vehicle file should exist.", f1.exists());
		File f2 = new File(utils.getOutputDirectory() + "digicoreVehicles.xml.gz");
		Assert.assertTrue("Vehicles container file should exist.", f2.exists());
		
		/* Check the vehicles container. */
		DigicoreVehicles vehicles = new DigicoreVehicles();
		new DigicoreVehiclesReader(vehicles).readFile(utils.getOutputDirectory() + "digicoreVehicles.xml.gz");
		Id<Vehicle> vid = Id.createVehicleId("10001");
		Assert.assertTrue("Vehicle 10001 not found.", vehicles.getVehicles().containsKey(vid));
		Assert.assertEquals("Wrong number of vehicles.", 1l, vehicles.getVehicles().size());
		
		/* Check the actual vehicle's activity chains. */
		DigicoreVehicle vehicle = vehicles.getVehicles().get(vid);
		Assert.assertEquals("Wrong number of chains.", 9l, vehicle.getChains().size());
		
	}
	
	
	
	
	
	
	private String[] getTestArguments(){
		String[] args = {
				utils.getClassInputDirectory() + "/test.csv.gz",
				utils.getOutputDirectory(),
				"Integration test",
				utils.getClassInputDirectory() + "/status.csv"
		};
		return args;
	}
	
	
	

}
