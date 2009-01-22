/* *********************************************************************** *
 * project: org.matsim.*
 * PlanAnalyzeTourModeChoiceSetTest.java
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

package org.matsim.population.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.BasicLeg;
import org.matsim.basic.v01.IdImpl;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.gbl.Gbl;
import org.matsim.population.Act;
import org.matsim.population.Person;
import org.matsim.population.PersonImpl;
import org.matsim.population.Plan;
import org.matsim.testcases.MatsimTestCase;

/**
 * Test class for {@link PlanAnalyzeTourModeChoiceSet}.
 * 
 * Contains illustrative examples for analysis of feasible mode chains as sketched in
 * 
 * Miller, E. J., M. J. Roorda and J. A. Carrasco (2005) A tour-based model of travel mode choice,
 * Transportation, 32 (4) 399–422, pp. 404 and 405.
 * 
 * @author meisterk
 *
 */
public class PlanAnalyzeTourModeChoiceSetTest extends MatsimTestCase {

	private Facilities facilities = null;
	
	private static final String CONFIGFILE = "test/scenarios/equil/config.xml";
	private static Logger log = Logger.getLogger(PlanAnalyzeTourModeChoiceSetTest.class);

	private HashMap<String, ArrayList<BasicLeg.Mode[]>> testCases = new HashMap<String, ArrayList<BasicLeg.Mode[]>>();
	
	protected void setUp() throws Exception {
		super.setUp();

		super.loadConfig(PlanAnalyzeTourModeChoiceSetTest.CONFIGFILE);

		log.info("Reading facilities xml file...");
		facilities = (Facilities)Gbl.getWorld().createLayer(Facilities.LAYER_TYPE, null);
		new MatsimFacilitiesReader(facilities).readFile(Gbl.getConfig().facilities().getInputFile());
		log.info("Reading facilities xml file...done.");
		
		////////////////////////////////////////////////////////////////
		//
		// 1 2 1
		//
		////////////////////////////////////////////////////////////////
		String testedActChainLocations = "1 2 1";
		ArrayList<BasicLeg.Mode[]> expectedTourModeOptions = new ArrayList<BasicLeg.Mode[]>();
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.car, BasicLeg.Mode.car});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.pt, BasicLeg.Mode.pt});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.pt, BasicLeg.Mode.walk});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.bike, BasicLeg.Mode.bike});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.walk, BasicLeg.Mode.pt});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.walk, BasicLeg.Mode.walk});
		testCases.put(testedActChainLocations, expectedTourModeOptions);

		////////////////////////////////////////////////////////////////
		//
		// 1 2 20 1
		//
		////////////////////////////////////////////////////////////////
		testedActChainLocations = "1 2 20 1";
		expectedTourModeOptions = new ArrayList<BasicLeg.Mode[]>();
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.car, BasicLeg.Mode.car, BasicLeg.Mode.car});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.pt, BasicLeg.Mode.pt, BasicLeg.Mode.pt});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.pt, BasicLeg.Mode.pt, BasicLeg.Mode.walk});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.pt, BasicLeg.Mode.walk, BasicLeg.Mode.pt});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.pt, BasicLeg.Mode.walk, BasicLeg.Mode.walk});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.bike, BasicLeg.Mode.bike, BasicLeg.Mode.bike});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.walk, BasicLeg.Mode.pt, BasicLeg.Mode.pt});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.walk, BasicLeg.Mode.pt, BasicLeg.Mode.walk});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.walk, BasicLeg.Mode.walk, BasicLeg.Mode.pt});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.walk, BasicLeg.Mode.walk, BasicLeg.Mode.walk});
		testCases.put(testedActChainLocations, expectedTourModeOptions);

		////////////////////////////////////////////////////////////////
		//
		// 1 2 1 2 1
		//
		////////////////////////////////////////////////////////////////

		testedActChainLocations = "1 2 1 2 1";
		expectedTourModeOptions = new ArrayList<BasicLeg.Mode[]>();
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.car, BasicLeg.Mode.car, BasicLeg.Mode.car, BasicLeg.Mode.car});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.car, BasicLeg.Mode.car, BasicLeg.Mode.pt, BasicLeg.Mode.pt});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.car, BasicLeg.Mode.car, BasicLeg.Mode.pt, BasicLeg.Mode.walk});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.car, BasicLeg.Mode.car, BasicLeg.Mode.bike, BasicLeg.Mode.bike});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.car, BasicLeg.Mode.car, BasicLeg.Mode.walk, BasicLeg.Mode.pt});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.car, BasicLeg.Mode.car, BasicLeg.Mode.walk, BasicLeg.Mode.walk});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.car, BasicLeg.Mode.pt, BasicLeg.Mode.pt, BasicLeg.Mode.car});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.car, BasicLeg.Mode.pt, BasicLeg.Mode.walk, BasicLeg.Mode.car});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.car, BasicLeg.Mode.walk, BasicLeg.Mode.pt, BasicLeg.Mode.car});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.car, BasicLeg.Mode.walk, BasicLeg.Mode.walk, BasicLeg.Mode.car});

		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.pt, BasicLeg.Mode.pt, BasicLeg.Mode.car, BasicLeg.Mode.car});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.pt, BasicLeg.Mode.pt, BasicLeg.Mode.pt, BasicLeg.Mode.pt});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.pt, BasicLeg.Mode.pt, BasicLeg.Mode.pt, BasicLeg.Mode.walk});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.pt, BasicLeg.Mode.pt, BasicLeg.Mode.bike, BasicLeg.Mode.bike});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.pt, BasicLeg.Mode.pt, BasicLeg.Mode.walk, BasicLeg.Mode.pt});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.pt, BasicLeg.Mode.pt, BasicLeg.Mode.walk, BasicLeg.Mode.walk});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.pt, BasicLeg.Mode.walk, BasicLeg.Mode.car, BasicLeg.Mode.car});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.pt, BasicLeg.Mode.walk, BasicLeg.Mode.pt, BasicLeg.Mode.pt});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.pt, BasicLeg.Mode.walk, BasicLeg.Mode.pt, BasicLeg.Mode.walk});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.pt, BasicLeg.Mode.walk, BasicLeg.Mode.bike, BasicLeg.Mode.bike});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.pt, BasicLeg.Mode.walk, BasicLeg.Mode.walk, BasicLeg.Mode.pt});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.pt, BasicLeg.Mode.walk, BasicLeg.Mode.walk, BasicLeg.Mode.walk});

		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.bike, BasicLeg.Mode.pt, BasicLeg.Mode.pt, BasicLeg.Mode.bike});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.bike, BasicLeg.Mode.pt, BasicLeg.Mode.walk, BasicLeg.Mode.bike});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.bike, BasicLeg.Mode.bike, BasicLeg.Mode.car, BasicLeg.Mode.car});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.bike, BasicLeg.Mode.bike, BasicLeg.Mode.pt, BasicLeg.Mode.pt});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.bike, BasicLeg.Mode.bike, BasicLeg.Mode.pt, BasicLeg.Mode.walk});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.bike, BasicLeg.Mode.bike, BasicLeg.Mode.bike, BasicLeg.Mode.bike});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.bike, BasicLeg.Mode.bike, BasicLeg.Mode.walk, BasicLeg.Mode.pt});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.bike, BasicLeg.Mode.bike, BasicLeg.Mode.walk, BasicLeg.Mode.walk});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.bike, BasicLeg.Mode.walk, BasicLeg.Mode.pt, BasicLeg.Mode.bike});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.bike, BasicLeg.Mode.walk, BasicLeg.Mode.walk, BasicLeg.Mode.bike});

		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.walk, BasicLeg.Mode.pt, BasicLeg.Mode.car, BasicLeg.Mode.car});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.walk, BasicLeg.Mode.pt, BasicLeg.Mode.pt, BasicLeg.Mode.pt});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.walk, BasicLeg.Mode.pt, BasicLeg.Mode.pt, BasicLeg.Mode.walk});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.walk, BasicLeg.Mode.pt, BasicLeg.Mode.bike, BasicLeg.Mode.bike});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.walk, BasicLeg.Mode.pt, BasicLeg.Mode.walk, BasicLeg.Mode.pt});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.walk, BasicLeg.Mode.pt, BasicLeg.Mode.walk, BasicLeg.Mode.walk});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.walk, BasicLeg.Mode.walk, BasicLeg.Mode.car, BasicLeg.Mode.car});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.walk, BasicLeg.Mode.walk, BasicLeg.Mode.pt, BasicLeg.Mode.pt});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.walk, BasicLeg.Mode.walk, BasicLeg.Mode.pt, BasicLeg.Mode.walk});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.walk, BasicLeg.Mode.walk, BasicLeg.Mode.bike, BasicLeg.Mode.bike});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.walk, BasicLeg.Mode.walk, BasicLeg.Mode.walk, BasicLeg.Mode.pt});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.walk, BasicLeg.Mode.walk, BasicLeg.Mode.walk, BasicLeg.Mode.walk});
		testCases.put(testedActChainLocations, expectedTourModeOptions);

		////////////////////////////////////////////////////////////////
		//
		// 1 2 1 3 1
		//
		////////////////////////////////////////////////////////////////
		testedActChainLocations = "1 2 1 3 1";
		expectedTourModeOptions = new ArrayList<BasicLeg.Mode[]>();
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.car, BasicLeg.Mode.car, BasicLeg.Mode.car, BasicLeg.Mode.car});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.car, BasicLeg.Mode.car, BasicLeg.Mode.pt, BasicLeg.Mode.pt});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.car, BasicLeg.Mode.car, BasicLeg.Mode.pt, BasicLeg.Mode.walk});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.car, BasicLeg.Mode.car, BasicLeg.Mode.bike, BasicLeg.Mode.bike});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.car, BasicLeg.Mode.car, BasicLeg.Mode.walk, BasicLeg.Mode.pt});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.car, BasicLeg.Mode.car, BasicLeg.Mode.walk, BasicLeg.Mode.walk});

		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.pt, BasicLeg.Mode.pt, BasicLeg.Mode.car, BasicLeg.Mode.car});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.pt, BasicLeg.Mode.pt, BasicLeg.Mode.pt, BasicLeg.Mode.pt});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.pt, BasicLeg.Mode.pt, BasicLeg.Mode.pt, BasicLeg.Mode.walk});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.pt, BasicLeg.Mode.pt, BasicLeg.Mode.bike, BasicLeg.Mode.bike});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.pt, BasicLeg.Mode.pt, BasicLeg.Mode.walk, BasicLeg.Mode.pt});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.pt, BasicLeg.Mode.pt, BasicLeg.Mode.walk, BasicLeg.Mode.walk});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.pt, BasicLeg.Mode.walk, BasicLeg.Mode.car, BasicLeg.Mode.car});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.pt, BasicLeg.Mode.walk, BasicLeg.Mode.pt, BasicLeg.Mode.pt});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.pt, BasicLeg.Mode.walk, BasicLeg.Mode.pt, BasicLeg.Mode.walk});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.pt, BasicLeg.Mode.walk, BasicLeg.Mode.bike, BasicLeg.Mode.bike});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.pt, BasicLeg.Mode.walk, BasicLeg.Mode.walk, BasicLeg.Mode.pt});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.pt, BasicLeg.Mode.walk, BasicLeg.Mode.walk, BasicLeg.Mode.walk});

		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.bike, BasicLeg.Mode.bike, BasicLeg.Mode.car, BasicLeg.Mode.car});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.bike, BasicLeg.Mode.bike, BasicLeg.Mode.pt, BasicLeg.Mode.pt});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.bike, BasicLeg.Mode.bike, BasicLeg.Mode.pt, BasicLeg.Mode.walk});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.bike, BasicLeg.Mode.bike, BasicLeg.Mode.bike, BasicLeg.Mode.bike});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.bike, BasicLeg.Mode.bike, BasicLeg.Mode.walk, BasicLeg.Mode.pt});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.bike, BasicLeg.Mode.bike, BasicLeg.Mode.walk, BasicLeg.Mode.walk});

		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.walk, BasicLeg.Mode.pt, BasicLeg.Mode.car, BasicLeg.Mode.car});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.walk, BasicLeg.Mode.pt, BasicLeg.Mode.pt, BasicLeg.Mode.pt});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.walk, BasicLeg.Mode.pt, BasicLeg.Mode.pt, BasicLeg.Mode.walk});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.walk, BasicLeg.Mode.pt, BasicLeg.Mode.bike, BasicLeg.Mode.bike});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.walk, BasicLeg.Mode.pt, BasicLeg.Mode.walk, BasicLeg.Mode.pt});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.walk, BasicLeg.Mode.pt, BasicLeg.Mode.walk, BasicLeg.Mode.walk});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.walk, BasicLeg.Mode.walk, BasicLeg.Mode.car, BasicLeg.Mode.car});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.walk, BasicLeg.Mode.walk, BasicLeg.Mode.pt, BasicLeg.Mode.pt});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.walk, BasicLeg.Mode.walk, BasicLeg.Mode.pt, BasicLeg.Mode.walk});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.walk, BasicLeg.Mode.walk, BasicLeg.Mode.bike, BasicLeg.Mode.bike});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.walk, BasicLeg.Mode.walk, BasicLeg.Mode.walk, BasicLeg.Mode.pt});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.walk, BasicLeg.Mode.walk, BasicLeg.Mode.walk, BasicLeg.Mode.walk});
		testCases.put(testedActChainLocations, expectedTourModeOptions);

		////////////////////////////////////////////////////////////////
		//
		// 1 2 3 4
		//
		////////////////////////////////////////////////////////////////
		testedActChainLocations = "1 2 3 4";
		expectedTourModeOptions = new ArrayList<BasicLeg.Mode[]>();
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.car, BasicLeg.Mode.car, BasicLeg.Mode.car});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.pt, BasicLeg.Mode.pt, BasicLeg.Mode.pt});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.pt, BasicLeg.Mode.pt, BasicLeg.Mode.walk});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.pt, BasicLeg.Mode.walk, BasicLeg.Mode.pt});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.pt, BasicLeg.Mode.walk, BasicLeg.Mode.walk});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.bike, BasicLeg.Mode.bike, BasicLeg.Mode.bike});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.walk, BasicLeg.Mode.pt, BasicLeg.Mode.pt});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.walk, BasicLeg.Mode.pt, BasicLeg.Mode.walk});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.walk, BasicLeg.Mode.walk, BasicLeg.Mode.pt});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.walk, BasicLeg.Mode.walk, BasicLeg.Mode.walk});
		testCases.put(testedActChainLocations, expectedTourModeOptions);

		////////////////////////////////////////////////////////////////
		//
		// 1 2 2 1
		//
		////////////////////////////////////////////////////////////////
		testedActChainLocations = "1 2 2 1";
		expectedTourModeOptions = new ArrayList<BasicLeg.Mode[]>();

		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.car, BasicLeg.Mode.car, BasicLeg.Mode.car});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.car, BasicLeg.Mode.pt, BasicLeg.Mode.car});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.car, BasicLeg.Mode.walk, BasicLeg.Mode.car});

		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.pt, BasicLeg.Mode.pt, BasicLeg.Mode.pt});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.pt, BasicLeg.Mode.pt, BasicLeg.Mode.walk});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.pt, BasicLeg.Mode.walk, BasicLeg.Mode.pt});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.pt, BasicLeg.Mode.walk, BasicLeg.Mode.walk});

		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.bike, BasicLeg.Mode.pt, BasicLeg.Mode.bike});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.bike, BasicLeg.Mode.bike, BasicLeg.Mode.bike});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.bike, BasicLeg.Mode.walk, BasicLeg.Mode.bike});

		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.walk, BasicLeg.Mode.pt, BasicLeg.Mode.pt});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.walk, BasicLeg.Mode.pt, BasicLeg.Mode.walk});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.walk, BasicLeg.Mode.walk, BasicLeg.Mode.pt});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.walk, BasicLeg.Mode.walk, BasicLeg.Mode.walk});

		testCases.put(testedActChainLocations, expectedTourModeOptions);

		////////////////////////////////////////////////////////////////
		//
		// 1 2 3 4 3 2 1
		//
		////////////////////////////////////////////////////////////////
		testedActChainLocations = "1 2 3 4 3 2 1";
		expectedTourModeOptions = new ArrayList<BasicLeg.Mode[]>();

		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.car, BasicLeg.Mode.car, BasicLeg.Mode.car, BasicLeg.Mode.car, BasicLeg.Mode.car, BasicLeg.Mode.car});

		int variableLegs = 2;
		for (int ii = 0; ii < (int) Math.pow(2, variableLegs); ii++) {
			BasicLeg.Mode[] combination = new BasicLeg.Mode[testedActChainLocations.split(" ").length - 1];
			combination[0] = combination[1] = combination[4] = combination[5] = BasicLeg.Mode.car;
			for (int jj = 0; jj < variableLegs ; jj++) {
				combination[jj + 2] = (((ii & ((int) Math.pow(2, variableLegs - (jj + 1)))) == 0)) ? BasicLeg.Mode.pt : BasicLeg.Mode.walk; 
			}
			expectedTourModeOptions.add(combination);
		}		
		
		variableLegs = 4;
		for (int ii = 0; ii < (int) Math.pow(2, variableLegs); ii++) {
			BasicLeg.Mode[] combination = new BasicLeg.Mode[testedActChainLocations.split(" ").length - 1];
			combination[0] = combination[5] = BasicLeg.Mode.car;
			for (int jj = 0; jj < variableLegs ; jj++) {
				combination[jj + 1] = (((ii & ((int) Math.pow(2, variableLegs - (jj + 1)))) == 0)) ? BasicLeg.Mode.pt : BasicLeg.Mode.walk; 
			}
			expectedTourModeOptions.add(combination);
		}
		
		variableLegs = 5;
		for (int ii = 0; ii < (int) Math.pow(2, variableLegs); ii++) {
			BasicLeg.Mode[] combination = new BasicLeg.Mode[testedActChainLocations.split(" ").length - 1];
			combination[0] = BasicLeg.Mode.pt;
			for (int jj = 0; jj < variableLegs; jj++) {
				combination[jj + 1] = (((ii & ((int) Math.pow(2, variableLegs - (jj + 1)))) == 0)) ? BasicLeg.Mode.pt : BasicLeg.Mode.walk; 
			}
			expectedTourModeOptions.add(combination);
		}

		variableLegs = 3;
		for (int ii = 0; ii < (int) Math.pow(2, variableLegs); ii++) {
			BasicLeg.Mode[] combination = new BasicLeg.Mode[testedActChainLocations.split(" ").length - 1];
			combination[0] = combination[5] = BasicLeg.Mode.bike;
			combination[1] = BasicLeg.Mode.pt;
			for (int jj = 0; jj < variableLegs; jj++) {
				combination[jj + 2] = (((ii & ((int) Math.pow(2, variableLegs - (jj + 1)))) == 0)) ? BasicLeg.Mode.pt : BasicLeg.Mode.walk; 
			}
			expectedTourModeOptions.add(combination);
		}

		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.bike, BasicLeg.Mode.bike, BasicLeg.Mode.pt, BasicLeg.Mode.pt, BasicLeg.Mode.bike, BasicLeg.Mode.bike});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.bike, BasicLeg.Mode.bike, BasicLeg.Mode.pt, BasicLeg.Mode.walk, BasicLeg.Mode.bike, BasicLeg.Mode.bike});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.bike, BasicLeg.Mode.bike, BasicLeg.Mode.bike, BasicLeg.Mode.bike, BasicLeg.Mode.bike, BasicLeg.Mode.bike});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.bike, BasicLeg.Mode.bike, BasicLeg.Mode.walk, BasicLeg.Mode.pt, BasicLeg.Mode.bike, BasicLeg.Mode.bike});
		expectedTourModeOptions.add(new BasicLeg.Mode[]{BasicLeg.Mode.bike, BasicLeg.Mode.bike, BasicLeg.Mode.walk, BasicLeg.Mode.walk, BasicLeg.Mode.bike, BasicLeg.Mode.bike});

		variableLegs = 3;
		for (int ii = 0; ii < (int) Math.pow(2, variableLegs); ii++) {
			BasicLeg.Mode[] combination = new BasicLeg.Mode[testedActChainLocations.split(" ").length - 1];
			combination[0] = combination[5] = BasicLeg.Mode.bike;
			combination[1] = BasicLeg.Mode.walk;
			for (int jj = 0; jj < variableLegs; jj++) {
				combination[jj + 2] = (((ii & ((int) Math.pow(2, variableLegs - (jj + 1)))) == 0)) ? BasicLeg.Mode.pt : BasicLeg.Mode.walk; 
			}
			expectedTourModeOptions.add(combination);
		}

		variableLegs = 5;
		for (int ii = 0; ii < (int) Math.pow(2, variableLegs); ii++) {
			BasicLeg.Mode[] combination = new BasicLeg.Mode[testedActChainLocations.split(" ").length - 1];
			combination[0] = BasicLeg.Mode.walk;
			for (int jj = 0; jj < variableLegs; jj++) {
				combination[jj + 1] = (((ii & ((int) Math.pow(2, variableLegs - (jj + 1)))) == 0)) ? BasicLeg.Mode.pt : BasicLeg.Mode.walk; 
			}
			expectedTourModeOptions.add(combination);
		}

		testCases.put(testedActChainLocations, expectedTourModeOptions);
		
	}

	public void testRun() {

		PlanAnalyzeTourModeChoiceSet testee = new PlanAnalyzeTourModeChoiceSet();
		EnumSet<BasicLeg.Mode> possibleModes = EnumSet.of(BasicLeg.Mode.walk, BasicLeg.Mode.bike, BasicLeg.Mode.pt, BasicLeg.Mode.car);
		testee.setModeSet(possibleModes);

		Person person = new PersonImpl(new IdImpl("1000"));
		for (Entry<String, ArrayList<BasicLeg.Mode[]>> entry : testCases.entrySet()) {

			String facString  = entry.getKey();
			log.info("Testing location sequence: " + facString);

			Plan plan = new Plan(person);

			String[] facIdSequence = facString.split(" ");
			for (int aa=0; aa < facIdSequence.length; aa++) {
				Act act = plan.createAct("actOnLink" + facIdSequence[aa], facilities.getFacilities().get(new IdImpl(facIdSequence[aa])));
				act.setEndTime(10*3600);
				act.setFacility(facilities.getFacilities().get(new IdImpl(facIdSequence[aa])));
				if (aa != (facIdSequence.length - 1)) {
					plan.createLeg(BasicLeg.Mode.undefined);
				}
			}
			testee.run(plan);

			ArrayList<BasicLeg.Mode[]> actual = testee.getResult();
			assertEquals(entry.getValue().size(), actual.size());
			assertTrue(Arrays.deepEquals(
					(BasicLeg.Mode[][]) entry.getValue().toArray(new BasicLeg.Mode[0][0]), 
					(BasicLeg.Mode[][]) actual.toArray(new BasicLeg.Mode[0][0])));

		}

	}

}
