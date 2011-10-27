/* *********************************************************************** *
 * project: org.matsim.*
 * MATSim4UrbanSimTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.contrib.matsim4urbansim.matsim;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.contrib.matsim4opus.matsim4urbansim.MATSim4Urbansim;
import org.matsim.contrib.matsim4urbansim.matsimTestData.GenerateOPUSTestEnvironment;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.testcases.MatsimTestUtils;


/**
 * @author thomas
 *
 */
public class MATSim4UrbanSimTest extends MatsimTestCase{
	
	private static final Logger log = Logger.getLogger(MATSim4UrbanSimTest.class);
	private GenerateOPUSTestEnvironment gote = null;
	
	@Rule 
	public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
	public void testMATSimConfig(){
			
		log.info("Starting testMATSimConfig run: Testing if MATSim config is valid.");
		String matsimConfigPath = prepareTest( Boolean.TRUE );
		
		boolean status = testRun(matsimConfigPath);
		
		Assert.assertTrue( status );
		
		// remove temp directories
		if(gote != null)
			gote.cleanOPUSTestEnvironment();
		
		log.info("End of testMATSimConfig.");
	}
	
	@Test
	public void testMATSimRun(){
		log.info("Starting testMATSimRun run: Testing if MATSim run is passes through.");
		String matsimConfigPath = prepareTest( Boolean.FALSE );
		
		boolean status = testRun(matsimConfigPath);
		
		Assert.assertTrue( status );

		// remove temp directories
		if(gote != null)
			gote.cleanOPUSTestEnvironment();
		
		log.info("End of testMATSimRun.");
	}
	
	/**
	 * preparing MATSim test run
	 * @param configName name of MATSim config file
	 */
	private String prepareTest(boolean isTestRun){
		
		gote = new GenerateOPUSTestEnvironment(isTestRun);
		String matsimConfigPath = gote.createOPUSTestEnvironment();
		
		return matsimConfigPath;
	}
	
	/**
	 * running MATSim with different configuration files
	 * @param matsimConfigPath points to the location of the MATSim config
	 */
	private boolean testRun(String matsimConfigPath){
		log.info("Starting MATSim4UrbanSim with args = " + matsimConfigPath);
		String [] args = new String[]{matsimConfigPath}; // create program arguments for MATSim
		
		
		MATSim4Urbansim.main(args);
		boolean status = MATSim4Urbansim.getRunStatus();
		
		return status;
	}

}

