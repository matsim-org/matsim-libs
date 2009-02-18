/* *********************************************************************** *
 * project: org.matsim.*
 * PlanAnalyzeSubtoursTest.java
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

/**
 * Test class for {@link PlanAnalyzeSubtours}.
 * 
 * Contains illustrative examples for subtour analysis.
 * 
 * @author meisterk
 *
 */
public class PlanAnalyzeSubtoursTest extends MatsimTestCase {

	private Facilities facilities = null;

	private static final String CONFIGFILE = "test/scenarios/equil/config.xml";

	private static Logger log = Logger.getLogger(PlanAnalyzeSubtoursTest.class);

	protected void setUp() throws Exception {

		super.setUp();

		super.loadConfig(PlanAnalyzeSubtoursTest.CONFIGFILE);

		log.info("Reading facilities xml file...");
		facilities = new Facilities();
		new MatsimFacilitiesReader(facilities).readFile(Gbl.getConfig().facilities().getInputFile());
		log.info("Reading facilities xml file...done.");
	}

	public void testRun() throws Exception {

		PlanAnalyzeSubtours testee = new PlanAnalyzeSubtours();

		Person person = new PersonImpl(new IdImpl("1000"));

		// test different types of activity plans
		HashMap<String, String> expectedSubtourIndexations = new HashMap<String, String>();
		HashMap<String, Integer> expectedNumSubtours = new HashMap<String, Integer>();
		
		String testedActChainLocations = "1 2 1";
		expectedSubtourIndexations.put(testedActChainLocations, "0 0");
		expectedNumSubtours.put(testedActChainLocations, 1);
		
		testedActChainLocations = "1 2 20 1";
		expectedSubtourIndexations.put(testedActChainLocations, "0 0 0");
		expectedNumSubtours.put(testedActChainLocations, 1);

		testedActChainLocations = "1 2 1 2 1";
		expectedSubtourIndexations.put(testedActChainLocations, "0 0 1 1");
		expectedNumSubtours.put(testedActChainLocations, 2);

		testedActChainLocations = "1 2 1 3 1";
		expectedSubtourIndexations.put(testedActChainLocations, "0 0 1 1");
		expectedNumSubtours.put(testedActChainLocations, 2);

		testedActChainLocations = "1 2 2 1";
		expectedSubtourIndexations.put(testedActChainLocations, "1 0 1");
		expectedNumSubtours.put(testedActChainLocations, 2);
		
		testedActChainLocations = "1 2 2 2 2 2 2 2 1";
		expectedSubtourIndexations.put(testedActChainLocations, "6 0 1 2 3 4 5 6");
		expectedNumSubtours.put(testedActChainLocations, 7);

		testedActChainLocations = "1 2 3 2 1";
		expectedSubtourIndexations.put(testedActChainLocations, "1 0 0 1");
		expectedNumSubtours.put(testedActChainLocations, 2);

		testedActChainLocations = "1 2 3 4 3 2 1";
		expectedSubtourIndexations.put(testedActChainLocations, "2 1 0 0 1 2");
		expectedNumSubtours.put(testedActChainLocations, 3);

		testedActChainLocations = "1 2 14 2 14 2 1";
		expectedSubtourIndexations.put(testedActChainLocations, "2 0 0 1 1 2");
		expectedNumSubtours.put(testedActChainLocations, 3);

		testedActChainLocations = "1 2 14 14 2 14 2 1";
		expectedSubtourIndexations.put(testedActChainLocations, "3 1 0 1 2 2 3");
		expectedNumSubtours.put(testedActChainLocations, 4);

		testedActChainLocations = "1 2 3 4 3 2 5 4 5 1";
		expectedSubtourIndexations.put(testedActChainLocations, "3 1 0 0 1 3 2 2 3");
		expectedNumSubtours.put(testedActChainLocations, 4);

		testedActChainLocations = "1 2 3 2 3 2 1 2 1";
		expectedSubtourIndexations.put(testedActChainLocations, "2 0 0 1 1 2 3 3");
		expectedNumSubtours.put(testedActChainLocations, 4);

		testedActChainLocations = "1 1 1 1 1 2 1";
		expectedSubtourIndexations.put(testedActChainLocations, "0 1 2 3 4 4");
		expectedNumSubtours.put(testedActChainLocations, 5);

		testedActChainLocations = "1 2 1 1";
		expectedSubtourIndexations.put(testedActChainLocations, "0 0 1");
		expectedNumSubtours.put(testedActChainLocations, 2);

		testedActChainLocations = "1 2 3 4";
		expectedSubtourIndexations.put(
				testedActChainLocations, 
						Integer.toString(PlanAnalyzeSubtours.UNDEFINED) + " " + 
						Integer.toString(PlanAnalyzeSubtours.UNDEFINED) + " " + 
						Integer.toString(PlanAnalyzeSubtours.UNDEFINED));
		expectedNumSubtours.put(testedActChainLocations, 0);

		testedActChainLocations = "1 2 2 3 2 2 2 1 4 1";
		expectedSubtourIndexations.put(testedActChainLocations, "4 0 1 1 2 3 4 5 5");
		expectedNumSubtours.put(testedActChainLocations, 6);
		
		testedActChainLocations = "1 2 3 4 3 1";
		expectedSubtourIndexations.put(testedActChainLocations, "1 1 0 0 1");
		expectedNumSubtours.put(testedActChainLocations, 2);
		
		for (Entry<String, String> entry: expectedSubtourIndexations.entrySet()) {
			String facString  = entry.getKey();
			log.info("Testing location sequence: " + facString);

			Plan plan = new Plan(person);

			String[] facIdSequence = facString.split(" ");
			for (int aa=0; aa < facIdSequence.length; aa++) {
				Act act = plan.createAct("actOnLink" + facIdSequence[aa], facilities.getFacilities().get(new IdImpl(facIdSequence[aa])));
				act.setEndTime(10*3600);
				act.setFacility(facilities.getFacilities().get(new IdImpl(facIdSequence[aa])));
				if (aa != (facIdSequence.length - 1)) {
					plan.createLeg(BasicLeg.Mode.car);
				}
			}
			testee.run(plan);
			
			StringBuilder builder = new StringBuilder();
			for (int value : testee.getSubtourIndexation()) {
				builder.append(Integer.toString(value));
				builder.append(' ');
			}
			String actualSubtourIndexation = builder.substring(0, builder.length() - 1);
			assertEquals(entry.getValue(), actualSubtourIndexation);
			
			assertEquals(expectedNumSubtours.get(facString).intValue(), testee.getNumSubtours());
		}

	}

}
