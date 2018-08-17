/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.contrib.matsim4urbansim.run;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.contrib.matrixbasedptrouter.utils.CreateTestNetwork;
import org.matsim.contrib.matsim4urbansim.config.CreateTestM4UConfig;
import org.matsim.contrib.matsim4urbansim.matsim4urbansim.Zone2ZoneImpedancesControlerListener;
import org.matsim.contrib.matsim4urbansim.utils.io.writer.UrbanSimParcelCSVWriter;
import org.matsim.contrib.matsim4urbansim.utils.io.writer.UrbanSimPersonCSVWriter;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;


/**
 * @author nagel
 *
 */
public class MATSim4UrbanSimParcelIT {
	
	private static final Logger log = Logger.getLogger(MATSim4UrbanSimParcelIT.class);
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	
	/**
	 * This test makes sure that five csv files used as input for UrbanSim are written correctly
	 */
	@Test
	public void test() {
		String path = utils.getOutputDirectory() ;
		
		CreateUrbansimPopulation.createUrbanSimTestPopulation(path, 1) ;
		
		NetworkWriter writer = new NetworkWriter( CreateTestNetwork.createTestNetwork() ) ;
		final String networkFilename = path + "/network.xml.gz";
		writer.write( networkFilename);
		
		CreateTestM4UConfig creator = new CreateTestM4UConfig(path, networkFilename ) ;
		String filename = creator.generateM4UConfigV3() ;
		
		String[] args = { filename } ;
		MATSim4UrbanSimParcel.main( args ); 
				
		log.info("comparing parcels data ...");
		compareFilesByLinesInMemory(UrbanSimParcelCSVWriter.FILE_NAME) ;
		log.info("... done.");
		log.info("comparing accessibility indicators ...");
		compareFilesByLinesInMemory(UrbansimCellBasedAccessibilityCSVWriterV2.ACCESSIBILITY_INDICATORS) ;
		log.info("... done.");
		log.info("comparing zones data ...");
		compareFilesByLinesInMemory(UrbanSimZoneCSVWriterV2.FILE_NAME) ;
		log.info("... done.");
		log.info("comparing travel data ...");
		compareFilesByLinesInMemory(Zone2ZoneImpedancesControlerListener.FILE_NAME) ;
		log.info("... done.");
		log.info("comparing persons data ...");
		compareFilesByLinesInMemory(UrbanSimPersonCSVWriter.FILE_NAME) ;
		log.info("... done.");
	}


	private void compareFilesByLinesInMemory(String fileName) {
		String originalFileName = utils.getClassInputDirectory() + fileName ;
		log.info( "old: " + originalFileName ) ;
		List<String> expected = readAllLines(originalFileName);
		for ( String str : expected ) {
			System.err.println(str);
		}
		String revisedFileName = utils.getOutputDirectory() + fileName ;
		log.info( "new: " + revisedFileName ) ;
		List<String> actual = readAllLines(revisedFileName);
		for ( String str : actual ) {
			System.err.println(str);
		}
		Assert.assertEquals(expected, actual);
	}
	
	
	private static List<String> readAllLines(String filename) {
		try {
			return Files.readAllLines(Paths.get(filename), java.nio.charset.Charset.defaultCharset());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
