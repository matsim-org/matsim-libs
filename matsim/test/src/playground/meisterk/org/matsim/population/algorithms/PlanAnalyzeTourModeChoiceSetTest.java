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

package playground.meisterk.org.matsim.population.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.IdImpl;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.BasicLeg;
import org.matsim.population.Act;
import org.matsim.population.Person;
import org.matsim.population.PersonImpl;
import org.matsim.population.Plan;
import org.matsim.testcases.MatsimTestCase;

import playground.meisterk.org.matsim.basic.v01.ExtendedBasicLeg;

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
	
	private static final String CONFIGFILE = "test/scenarios/equil/config.xml";
	private static Logger log = Logger.getLogger(PlanAnalyzeTourModeChoiceSetTest.class);

	protected void setUp() throws Exception {
		super.setUp();
		super.loadConfig(PlanAnalyzeTourModeChoiceSetTest.CONFIGFILE);
	}

	public void testRun() {

		Facilities facilities = null;

		log.info("Reading facilities xml file...");
		facilities = (Facilities)Gbl.getWorld().createLayer(Facilities.LAYER_TYPE, null);
		new MatsimFacilitiesReader(facilities).readFile(Gbl.getConfig().facilities().getInputFile());
		log.info("Reading facilities xml file...done.");
		
		HashMap<String, ArrayList<ExtendedBasicLeg.Mode[]>> testCases = new HashMap<String, ArrayList<ExtendedBasicLeg.Mode[]>>();
		
		////////////////////////////////////////////////////////////////
		//
		// 1 2 1
		//
		////////////////////////////////////////////////////////////////
		String testedActChainLocations = "1 2 1";
		ArrayList<ExtendedBasicLeg.Mode[]> expectedTourModeOptions = new ArrayList<ExtendedBasicLeg.Mode[]>();
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.car});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.pt});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.walk});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.bike});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.pt});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.walk});
		testCases.put(testedActChainLocations, expectedTourModeOptions);

		////////////////////////////////////////////////////////////////
		//
		// 1 2 20 1
		//
		////////////////////////////////////////////////////////////////
		testedActChainLocations = "1 2 20 1";
		expectedTourModeOptions = new ArrayList<ExtendedBasicLeg.Mode[]>();
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.car});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.pt});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.walk});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.pt});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.walk});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.bike});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.pt});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.walk});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.pt});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.walk});
		testCases.put(testedActChainLocations, expectedTourModeOptions);

		////////////////////////////////////////////////////////////////
		//
		// 1 2 1 2 1
		//
		////////////////////////////////////////////////////////////////

		testedActChainLocations = "1 2 1 2 1";
		expectedTourModeOptions = new ArrayList<ExtendedBasicLeg.Mode[]>();
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.car});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.pt});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.walk});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.bike});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.pt});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.walk});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.car});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.car});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.car});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.car});

		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.car});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.pt});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.walk});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.bike});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.pt});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.walk});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.car});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.pt});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.walk});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.bike});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.pt});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.walk});

		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.bike});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.bike});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.car});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.pt});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.walk});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.bike});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.pt});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.walk});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.bike});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.bike});

		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.car});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.pt});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.walk});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.bike});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.pt});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.walk});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.car});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.pt});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.walk});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.bike});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.pt});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.walk});
		testCases.put(testedActChainLocations, expectedTourModeOptions);

		////////////////////////////////////////////////////////////////
		//
		// 1 2 1 3 1
		//
		////////////////////////////////////////////////////////////////
		testedActChainLocations = "1 2 1 3 1";
		expectedTourModeOptions = new ArrayList<ExtendedBasicLeg.Mode[]>();
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.car});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.pt});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.walk});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.bike});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.pt});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.walk});

		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.car});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.pt});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.walk});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.bike});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.pt});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.walk});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.car});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.pt});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.walk});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.bike});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.pt});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.walk});

		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.car});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.pt});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.walk});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.bike});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.pt});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.walk});

		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.car});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.pt});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.walk});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.bike});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.pt});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.walk});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.car});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.pt});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.walk});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.bike});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.pt});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.walk});
		testCases.put(testedActChainLocations, expectedTourModeOptions);

		////////////////////////////////////////////////////////////////
		//
		// 1 2 3 4
		//
		////////////////////////////////////////////////////////////////
		testedActChainLocations = "1 2 3 4";
		expectedTourModeOptions = new ArrayList<ExtendedBasicLeg.Mode[]>();
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.car});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.pt});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.walk});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.pt});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.walk});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.bike});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.pt});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.walk});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.pt});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.walk});
		testCases.put(testedActChainLocations, expectedTourModeOptions);

		////////////////////////////////////////////////////////////////
		//
		// 1 2 2 1
		//
		////////////////////////////////////////////////////////////////
		testedActChainLocations = "1 2 2 1";
		expectedTourModeOptions = new ArrayList<ExtendedBasicLeg.Mode[]>();

		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.car});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.car});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.car});

		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.pt});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.walk});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.pt});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.walk});

		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.bike});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.bike});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.bike});

		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.pt});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.walk});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.pt});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.walk});

		testCases.put(testedActChainLocations, expectedTourModeOptions);

		////////////////////////////////////////////////////////////////
		//
		// 1 2 3 4 3 2 1
		//
		////////////////////////////////////////////////////////////////
		testedActChainLocations = "1 2 3 4 3 2 1";
		expectedTourModeOptions = new ArrayList<ExtendedBasicLeg.Mode[]>();

		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.car, ExtendedBasicLeg.Mode.car});

		int variableLegs = 2;
		for (int ii = 0; ii < (int) Math.pow(2, variableLegs); ii++) {
			ExtendedBasicLeg.Mode[] combination = new ExtendedBasicLeg.Mode[testedActChainLocations.split(" ").length - 1];
			combination[0] = combination[1] = combination[4] = combination[5] = ExtendedBasicLeg.Mode.car;
			for (int jj = 0; jj < variableLegs ; jj++) {
				combination[jj + 2] = (((ii & ((int) Math.pow(2, variableLegs - (jj + 1)))) == 0)) ? ExtendedBasicLeg.Mode.pt : ExtendedBasicLeg.Mode.walk; 
			}
			expectedTourModeOptions.add(combination);
		}		
		
		variableLegs = 4;
		for (int ii = 0; ii < (int) Math.pow(2, variableLegs); ii++) {
			ExtendedBasicLeg.Mode[] combination = new ExtendedBasicLeg.Mode[testedActChainLocations.split(" ").length - 1];
			combination[0] = combination[5] = ExtendedBasicLeg.Mode.car;
			for (int jj = 0; jj < variableLegs ; jj++) {
				combination[jj + 1] = (((ii & ((int) Math.pow(2, variableLegs - (jj + 1)))) == 0)) ? ExtendedBasicLeg.Mode.pt : ExtendedBasicLeg.Mode.walk; 
			}
			expectedTourModeOptions.add(combination);
		}
		
		variableLegs = 5;
		for (int ii = 0; ii < (int) Math.pow(2, variableLegs); ii++) {
			ExtendedBasicLeg.Mode[] combination = new ExtendedBasicLeg.Mode[testedActChainLocations.split(" ").length - 1];
			combination[0] = ExtendedBasicLeg.Mode.pt;
			for (int jj = 0; jj < variableLegs; jj++) {
				combination[jj + 1] = (((ii & ((int) Math.pow(2, variableLegs - (jj + 1)))) == 0)) ? ExtendedBasicLeg.Mode.pt : ExtendedBasicLeg.Mode.walk; 
			}
			expectedTourModeOptions.add(combination);
		}

		variableLegs = 3;
		for (int ii = 0; ii < (int) Math.pow(2, variableLegs); ii++) {
			ExtendedBasicLeg.Mode[] combination = new ExtendedBasicLeg.Mode[testedActChainLocations.split(" ").length - 1];
			combination[0] = combination[5] = ExtendedBasicLeg.Mode.bike;
			combination[1] = ExtendedBasicLeg.Mode.pt;
			for (int jj = 0; jj < variableLegs; jj++) {
				combination[jj + 2] = (((ii & ((int) Math.pow(2, variableLegs - (jj + 1)))) == 0)) ? ExtendedBasicLeg.Mode.pt : ExtendedBasicLeg.Mode.walk; 
			}
			expectedTourModeOptions.add(combination);
		}

		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.bike});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.bike});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.bike});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.bike});
		expectedTourModeOptions.add(new ExtendedBasicLeg.Mode[]{ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.bike});

		variableLegs = 3;
		for (int ii = 0; ii < (int) Math.pow(2, variableLegs); ii++) {
			ExtendedBasicLeg.Mode[] combination = new ExtendedBasicLeg.Mode[testedActChainLocations.split(" ").length - 1];
			combination[0] = combination[5] = ExtendedBasicLeg.Mode.bike;
			combination[1] = ExtendedBasicLeg.Mode.walk;
			for (int jj = 0; jj < variableLegs; jj++) {
				combination[jj + 2] = (((ii & ((int) Math.pow(2, variableLegs - (jj + 1)))) == 0)) ? ExtendedBasicLeg.Mode.pt : ExtendedBasicLeg.Mode.walk; 
			}
			expectedTourModeOptions.add(combination);
		}

		variableLegs = 5;
		for (int ii = 0; ii < (int) Math.pow(2, variableLegs); ii++) {
			ExtendedBasicLeg.Mode[] combination = new ExtendedBasicLeg.Mode[testedActChainLocations.split(" ").length - 1];
			combination[0] = ExtendedBasicLeg.Mode.walk;
			for (int jj = 0; jj < variableLegs; jj++) {
				combination[jj + 1] = (((ii & ((int) Math.pow(2, variableLegs - (jj + 1)))) == 0)) ? ExtendedBasicLeg.Mode.pt : ExtendedBasicLeg.Mode.walk; 
			}
			expectedTourModeOptions.add(combination);
		}

		testCases.put(testedActChainLocations, expectedTourModeOptions);

		PlanAnalyzeTourModeChoiceSet testee = new PlanAnalyzeTourModeChoiceSet();
		EnumSet<ExtendedBasicLeg.Mode> possibleModes = EnumSet.of(ExtendedBasicLeg.Mode.walk, ExtendedBasicLeg.Mode.bike, ExtendedBasicLeg.Mode.pt, ExtendedBasicLeg.Mode.car);
		testee.setModeSet(possibleModes);

		Person person = new PersonImpl(new IdImpl("1000"));
		for (Entry<String, ArrayList<ExtendedBasicLeg.Mode[]>> entry : testCases.entrySet()) {

			String facString  = entry.getKey();
			log.info("Testing location sequence: " + facString);

			Plan plan = new org.matsim.population.PlanImpl(person);

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

			ArrayList<ExtendedBasicLeg.Mode[]> actual = testee.getResult();
			assertEquals(entry.getValue().size(), actual.size());
			assertTrue(Arrays.deepEquals(
					(ExtendedBasicLeg.Mode[][]) entry.getValue().toArray(new ExtendedBasicLeg.Mode[0][0]), 
					(ExtendedBasicLeg.Mode[][]) actual.toArray(new ExtendedBasicLeg.Mode[0][0])));

		}

	}

}
