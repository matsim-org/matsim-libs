/* *********************************************************************** *
 * project: org.matsim.*
 * ReadFromUrbansimParcelModelTest.java
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
package playground.tnicolai.urbansim.matsimTest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.testcases.MatsimTestUtils;

import playground.tnicolai.urbansim.constants.Constants;
import playground.tnicolai.urbansim.utils.MATSimConfigObject;
import playground.tnicolai.urbansim.utils.io.ReadFromUrbansimParcelModel;
import playground.tnicolai.urbansim.utils.io.TempDirectoryUtil;

/**
 * @author thomas
 *
 */
public class ReadFromUrbansimParcelModelTest extends MatsimTestCase{

	private static final Logger log = Logger.getLogger(ReadFromUrbansimParcelModelTest.class);
	private int popSize = -1;
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
	// Tests if facilities are created correctly
	public void testReadFromUrbansimParcelModel(){
		log.info("Starting testReadFromUrbansimParcelModel1 run: Testing computation of UrbanSim zones.");
		prepareTest("testReadFromUrbansimParcelModel1", 1);		
	}
	
	@Test
	// Tests if new population is created correctly
	public void testReadFromUrbansimParcelModel2(){
		log.info("Starting testReadFromUrbansimParcelModel2 run: Testing computation of UrbanSim zones.");
		prepareTest("testReadFromUrbansimParcelModel2", 2);	
	}
	
	@Test
	// Tests if old population is recognized correctly
	public void testReadFromUrbansimParcelModel3(){
		log.info("Starting testReadFromUrbansimParcelModel3 run: Testing computation of UrbanSim zones.");
		prepareTest("testReadFromUrbansimParcelModel3", 3);	
	}
	
	/**
	 * preparing ReadFromUrbansimParcelModel test run
	 * @param testRunName name of current test case
	 */
	private void prepareTest(String testRunName, int testNr){
		
		Constants.setOpusHomeDirectory( System.getProperty("java.io.tmpdir") );
		// create temp directories
		TempDirectoryUtil.createOPUSDirectories();
		
		// preparing input test file
		int dummyYear = 2000;
		String testFileDirectory = Constants.OPUS_MATSIM_TEMPORARY_DIRECTORY;
		String testFileName = null;
		
		switch (testNr) {
		case 1:
			testFileName = Constants.URBANSIM_PARCEL_DATASET_TABLE + dummyYear + Constants.FILE_TYPE_TAB;
			createParcelInputTestFile(testFileDirectory, testFileName);
			// running ReadFromUrbansimParcelModel generating zones and facilities
			ActivityFacilitiesImpl zones = testRunPracels( dummyYear );
			Assert.assertTrue( validateResult(zones) );
			break;

		case 2:
			testFileName = Constants.URBANSIM_PERSON_DATASET_TABLE + dummyYear + Constants.FILE_TYPE_TAB;
			createPersonInputTestFile(testFileDirectory, testFileName);
			// running ReadFromUrbansimParcelModel generating population
			boolean result = testRunPopulation( dummyYear );
			Assert.assertTrue(result);
			break;
			
		case 3:
			// TODO
		}
		
		// remove temp directories
		TempDirectoryUtil.cleaningUpOPUSDirectories();
		log.info("End of " + testRunName + ".");
	}
	
	/**
	 * validating test result
	 * @param zones ActivityFacilitiesImpl result of test run
	 * @return true if validation successful, false otherwise
	 */
	private boolean validateResult(ActivityFacilitiesImpl zones){
		
		Id zone_ID;
		ActivityFacilityImpl af;
		Coord coord;
		
		boolean zone1 = false;
		boolean zone2 = false; 
		boolean zone3 = false; 
		boolean zone4 = false;
		
		for( Entry<Id, ActivityFacilityImpl> entry : (zones.getFacilities()).entrySet() ){
			zone_ID = entry.getKey();
			af = entry.getValue();
			coord = af.getCoord();

			if(zone_ID.equals(new IdImpl("1")))
				zone1 = (coord.getX() == 50) && (coord.getY() == 150);
			else if(zone_ID.equals(new IdImpl("2")))
				zone2 = (coord.getX() == 150) && (coord.getY() == 150);
			else if(zone_ID.equals(new IdImpl("3")))
				zone3 = (coord.getX() == 50) && (coord.getY() == 50);
			else if(zone_ID.equals(new IdImpl("4")))
				zone4 = (coord.getX() == 150) && (coord.getY() == 50);
				
		}
		return zone1 && zone2 && zone3 && zone4;
	}
	
	/**
	 * Runns ReadFromUrbansimParcelModel
	 * @param year dummy year only needed to initialize ReadFromUrbansimParcelModel
	 * @return ActivityFacilitiesImpl constructed zones
	 */
	private ActivityFacilitiesImpl testRunPracels(int year){
		log.info("Running ReadFromUrbansimParcelModel with argument year = " + year);
		// get the data from test urbansim parcels
		ReadFromUrbansimParcelModel readFromUrbansim = new ReadFromUrbansimParcelModel( year );
		// read urbansim facilities (these are simply those entities that have the coordinates!)
		ActivityFacilitiesImpl facilities = new ActivityFacilitiesImpl("urbansim locations (gridcells _or_ parcels _or_ ...)");
		ActivityFacilitiesImpl zones      = new ActivityFacilitiesImpl("urbansim zones");
		readFromUrbansim.readFacilities(facilities, zones);
		// return constructed zones for validation
		return zones;
	}
	
	/**
	 * Runs ReadFromUrbansimParcelModel 
	 * @param year dummy year only needed to initialize ReadFromUrbansimParcelModel
	 * @return returns true if number of generated population aproximatly equals 
	 * 		   "sample rate * number of urbansim persons".
	 */
	private boolean testRunPopulation(int year){
		
		log.info("Running ReadFromUrbansimParcelModel with argument year = " + year);
		
		double sampleRate = 0.1;
		
		// get the data from urbansim (parcels and persons)
		ReadFromUrbansimParcelModel readFromUrbansim = new ReadFromUrbansimParcelModel( year );

		// read urbansim facilities (these are simply those entities that have the coordinates!)
		ActivityFacilitiesImpl facilities = new ActivityFacilitiesImpl("dummy locations");
		ActivityFacilitiesImpl zones      = new ActivityFacilitiesImpl("dummy zones");
		readFromUrbansim.readFacilities(facilities, zones);
		
		Population oldPopulation = null;
		Population newPopulation = new ScenarioImpl().getPopulation();
		// read urbansim persons.  Generates hwh acts as side effect
		readFromUrbansim.readPersons( oldPopulation, newPopulation, facilities, null, sampleRate );
		
		int populationCountLowerBorder = (int) (this.popSize * 0.05);
		int populationCountUpperBorder = (int) (this.popSize * 0.2);
		
		int populationActualCount = newPopulation.getPersons().size();
		
		return (populationActualCount >= populationCountLowerBorder && populationActualCount <= populationCountUpperBorder);
	}
	
	/**
	 * creates person table
	 * @param directory poits to the directory were to store the test input file
	 * @param fileName name of the test input file
	 * @return boolean indicates wether the creation of the test input file was successful
	 */
	private boolean createPersonInputTestFile(String directory, String fileName){
		
		int personID = 1;
		int home_ID = 1;
		int work_ID = 0;
		
		// create header line
		StringBuffer testFileContent = new StringBuffer();
		
		// create header line
		testFileContent.append( Constants.PERSON_ID + Constants.TAB + 
								Constants.PARCEL_ID_HOME + Constants.TAB + 
								Constants.PARCEL_ID_WORK + Constants.NEW_LINE);
		
		// build person table >> person_id	parcel_id_home	parcel_id_work
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		home_ID++;
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		home_ID++;
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		home_ID++;
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		home_ID++;
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		home_ID++;
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		home_ID++;
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		home_ID++;
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		home_ID++;
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		home_ID++;
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		home_ID++;
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID++ + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		testFileContent.append( personID + Constants.TAB + home_ID + Constants.TAB + getNextWorkID(work_ID++) + Constants.NEW_LINE);
		
		this.popSize = personID;
		
		return testFileWiter(directory, fileName, testFileContent);
	}
	
	/**
	 * returns id's from 1 - 11 via modulo operation
	 * @param workID
	 * @return
	 */
	private int getNextWorkID(int workID){
		
		return (workID % 11)+1;
	}
	
	/**
	 * create a test parcel data set table 
	 * @param directory poits to the directory were to store the test input file
	 * @param fileName name of the test input file
	 * @return boolean indicates wether the creation of the test input file was successful
	 */
	private boolean createParcelInputTestFile(String directory, String fileName){
		
		// test file will contain four zones:
		// zone1 will have 4 facilities located at coordinates
		//		fac1: 0,200, fac2: 100,200, fac3: 0,100 and fac4: 100,100
		// 		expected center of zone1 is 50,150
		// zone2 will have 2 facilities located at coordinates
		//		fac1: 200,200 and fac2: 100,100
		// 		expected center of zone2 is 150,150
		// zone3 will have 4 facilities located at coordinates
		//		fac1: 50,100, fac2: 100,50, fac3: 50,0 and fac4: 0,50
		// 		expected center of zone3 is 50,50
		// zone4 will have 1 facility located at coordinate
		//		fac1: 150,50
		// 		expected center of zone4 is 150,50
		
		StringBuffer testFileContent = new StringBuffer();
		
		// create header line
		testFileContent.append( Constants.PARCEL_ID + Constants.TAB + 
								Constants.X_COORDINATE + Constants.TAB + 
								Constants.Y_COORDINATE + Constants.TAB + 
								Constants.ZONE_ID + Constants.NEW_LINE);
		
		// put in zone 1 >> pracel_id	x_coordinate	y_coordinate	zone_id
		testFileContent.append( "1" + Constants.TAB + "0" + Constants.TAB + "200" + Constants.TAB + "1" + Constants.NEW_LINE);
		testFileContent.append( "2" + Constants.TAB + "100" + Constants.TAB + "200" + Constants.TAB + "1" + Constants.NEW_LINE);
		testFileContent.append( "3" + Constants.TAB + "0" + Constants.TAB + "100" + Constants.TAB + "1" + Constants.NEW_LINE);
		testFileContent.append( "4" + Constants.TAB + "100" + Constants.TAB + "100" + Constants.TAB + "1" + Constants.NEW_LINE);
		
		// put in zone 2 >> pracel_id	x_coordinate	y_coordinate	zone_id
		testFileContent.append( "5" + Constants.TAB + "200" + Constants.TAB + "200" + Constants.TAB + "2" + Constants.NEW_LINE);
		testFileContent.append( "6" + Constants.TAB + "100" + Constants.TAB + "100" + Constants.TAB + "2" + Constants.NEW_LINE);
	
		// put in zone 3 >> pracel_id	x_coordinate	y_coordinate	zone_id
		testFileContent.append( "7" + Constants.TAB + "50" + Constants.TAB + "100" + Constants.TAB + "3" + Constants.NEW_LINE);
		testFileContent.append( "8" + Constants.TAB + "100" + Constants.TAB + "50" + Constants.TAB + "3" + Constants.NEW_LINE);
		testFileContent.append( "9" + Constants.TAB + "50" + Constants.TAB + "0" + Constants.TAB + "3" + Constants.NEW_LINE);
		testFileContent.append( "10" + Constants.TAB + "0" + Constants.TAB + "50" + Constants.TAB + "3" + Constants.NEW_LINE);
		
		// put in zone 4 >> pracel_id	x_coordinate	y_coordinate	zone_id
		testFileContent.append( "11" + Constants.TAB + "150" + Constants.TAB + "50" + Constants.TAB + "4" + Constants.NEW_LINE);
		
		return testFileWiter(directory, fileName, testFileContent);
	}
	
	/**
	 * writes the content of a created test file into a file
	 * @param directory
	 * @param fileName
	 * @param testFileContent
	 * @return ture if successful, flase otherwise
	 */
	private boolean testFileWiter(String directory, String fileName, StringBuffer testFileContent){
		
		FileWriter fileStream;
		BufferedWriter bufferedOutput;
		// write test input file into temp directory
		try{
			fileStream = new FileWriter(new File(directory, fileName));
			bufferedOutput = new BufferedWriter(fileStream);
			
			log.info("Created test input file:");
			log.info( Constants.NEW_LINE + testFileContent.toString() );
			
			bufferedOutput.write( testFileContent.toString() );
			bufferedOutput.flush();
			bufferedOutput.close();
			fileStream.close();
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
			return false;
		}
		return true;
		
	}
}

