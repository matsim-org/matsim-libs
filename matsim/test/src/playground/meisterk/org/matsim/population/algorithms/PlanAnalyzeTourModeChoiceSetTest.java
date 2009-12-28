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
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.world.Layer;
import org.matsim.world.Location;

import playground.meisterk.org.matsim.config.groups.MeisterkConfigGroup;

/**
 * Test class for {@link PlanAnalyzeTourModeChoiceSet}.
 * 
 * Contains illustrative examples for analysis of feasible mode chains. See documentation <a href=http://matsim.org/node/267">here</a>.
 * @author meisterk
 *
 */
public class PlanAnalyzeTourModeChoiceSetTest extends MatsimTestCase {

	private static final String CONFIGFILE = "test/scenarios/equil/config.xml";
	private static Logger log = Logger.getLogger(PlanAnalyzeTourModeChoiceSetTest.class);

	private Config config;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.config = super.loadConfig(PlanAnalyzeTourModeChoiceSetTest.CONFIGFILE);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		this.config = null;
	}

	public void testFacilitiesBased() {

		// load data
		log.info("Reading facilities xml file...");
		ActivityFacilitiesImpl facilities = new ActivityFacilitiesImpl();
		new MatsimFacilitiesReader(facilities).readFile(this.config.facilities().getInputFile());
		log.info("Reading facilities xml file...done.");

		// run
		this.runDemo((Layer) facilities);

	}

	public void testNetworkBased() {

		// load data
		log.info("Reading network xml file...");
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(this.config.network().getInputFile());
		log.info("Reading network xml file...done.");

		// config
		this.config.planomat().setTripStructureAnalysisLayer(PlanomatConfigGroup.TripStructureAnalysisLayerOption.link);

		// run
		this.runDemo((Layer) network);

	}

	protected void runDemo(Layer layer) {

		HashMap<String, ArrayList<TransportMode[]>> testCases = new HashMap<String, ArrayList<TransportMode[]>>();

		////////////////////////////////////////////////////////////////
		//
		// 1 2 1
		//
		////////////////////////////////////////////////////////////////
		String testedActChainLocations = "1 2 1";
		ArrayList<TransportMode[]> expectedTourModeOptions = new ArrayList<TransportMode[]>();
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.car, TransportMode.car});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.pt, TransportMode.pt});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.pt, TransportMode.walk});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.bike, TransportMode.bike});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.walk, TransportMode.pt});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.walk, TransportMode.walk});
		testCases.put(testedActChainLocations, expectedTourModeOptions);

		////////////////////////////////////////////////////////////////
		//
		// 1 2 20 1
		//
		////////////////////////////////////////////////////////////////
		testedActChainLocations = "1 2 20 1";
		expectedTourModeOptions = new ArrayList<TransportMode[]>();
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.car, TransportMode.car, TransportMode.car});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.pt, TransportMode.pt, TransportMode.pt});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.pt, TransportMode.pt, TransportMode.walk});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.pt, TransportMode.walk, TransportMode.pt});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.pt, TransportMode.walk, TransportMode.walk});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.bike, TransportMode.bike, TransportMode.bike});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.walk, TransportMode.pt, TransportMode.pt});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.walk, TransportMode.pt, TransportMode.walk});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.walk, TransportMode.walk, TransportMode.pt});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.walk, TransportMode.walk, TransportMode.walk});
		testCases.put(testedActChainLocations, expectedTourModeOptions);

		////////////////////////////////////////////////////////////////
		//
		// 1 2 1 2 1
		//
		////////////////////////////////////////////////////////////////

		testedActChainLocations = "1 2 1 2 1";
		expectedTourModeOptions = new ArrayList<TransportMode[]>();
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.car, TransportMode.car, TransportMode.car, TransportMode.car});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.car, TransportMode.car, TransportMode.pt, TransportMode.pt});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.car, TransportMode.car, TransportMode.pt, TransportMode.walk});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.car, TransportMode.car, TransportMode.bike, TransportMode.bike});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.car, TransportMode.car, TransportMode.walk, TransportMode.pt});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.car, TransportMode.car, TransportMode.walk, TransportMode.walk});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.car, TransportMode.pt, TransportMode.pt, TransportMode.car});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.car, TransportMode.pt, TransportMode.walk, TransportMode.car});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.car, TransportMode.walk, TransportMode.pt, TransportMode.car});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.car, TransportMode.walk, TransportMode.walk, TransportMode.car});

		expectedTourModeOptions.add(new TransportMode[]{TransportMode.pt, TransportMode.pt, TransportMode.car, TransportMode.car});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.pt, TransportMode.pt, TransportMode.pt, TransportMode.pt});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.pt, TransportMode.pt, TransportMode.pt, TransportMode.walk});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.pt, TransportMode.pt, TransportMode.bike, TransportMode.bike});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.pt, TransportMode.pt, TransportMode.walk, TransportMode.pt});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.pt, TransportMode.pt, TransportMode.walk, TransportMode.walk});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.pt, TransportMode.walk, TransportMode.car, TransportMode.car});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.pt, TransportMode.walk, TransportMode.pt, TransportMode.pt});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.pt, TransportMode.walk, TransportMode.pt, TransportMode.walk});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.pt, TransportMode.walk, TransportMode.bike, TransportMode.bike});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.pt, TransportMode.walk, TransportMode.walk, TransportMode.pt});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.pt, TransportMode.walk, TransportMode.walk, TransportMode.walk});

		expectedTourModeOptions.add(new TransportMode[]{TransportMode.bike, TransportMode.pt, TransportMode.pt, TransportMode.bike});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.bike, TransportMode.pt, TransportMode.walk, TransportMode.bike});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.bike, TransportMode.bike, TransportMode.car, TransportMode.car});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.bike, TransportMode.bike, TransportMode.pt, TransportMode.pt});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.bike, TransportMode.bike, TransportMode.pt, TransportMode.walk});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.bike, TransportMode.bike, TransportMode.bike, TransportMode.bike});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.bike, TransportMode.bike, TransportMode.walk, TransportMode.pt});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.bike, TransportMode.bike, TransportMode.walk, TransportMode.walk});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.bike, TransportMode.walk, TransportMode.pt, TransportMode.bike});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.bike, TransportMode.walk, TransportMode.walk, TransportMode.bike});

		expectedTourModeOptions.add(new TransportMode[]{TransportMode.walk, TransportMode.pt, TransportMode.car, TransportMode.car});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.walk, TransportMode.pt, TransportMode.pt, TransportMode.pt});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.walk, TransportMode.pt, TransportMode.pt, TransportMode.walk});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.walk, TransportMode.pt, TransportMode.bike, TransportMode.bike});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.walk, TransportMode.pt, TransportMode.walk, TransportMode.pt});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.walk, TransportMode.pt, TransportMode.walk, TransportMode.walk});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.walk, TransportMode.walk, TransportMode.car, TransportMode.car});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.walk, TransportMode.walk, TransportMode.pt, TransportMode.pt});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.walk, TransportMode.walk, TransportMode.pt, TransportMode.walk});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.walk, TransportMode.walk, TransportMode.bike, TransportMode.bike});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.walk, TransportMode.walk, TransportMode.walk, TransportMode.pt});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.walk, TransportMode.walk, TransportMode.walk, TransportMode.walk});
		testCases.put(testedActChainLocations, expectedTourModeOptions);

		////////////////////////////////////////////////////////////////
		//
		// 1 2 1 3 1
		//
		////////////////////////////////////////////////////////////////
		testedActChainLocations = "1 2 1 3 1";
		expectedTourModeOptions = new ArrayList<TransportMode[]>();
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.car, TransportMode.car, TransportMode.car, TransportMode.car});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.car, TransportMode.car, TransportMode.pt, TransportMode.pt});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.car, TransportMode.car, TransportMode.pt, TransportMode.walk});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.car, TransportMode.car, TransportMode.bike, TransportMode.bike});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.car, TransportMode.car, TransportMode.walk, TransportMode.pt});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.car, TransportMode.car, TransportMode.walk, TransportMode.walk});

		expectedTourModeOptions.add(new TransportMode[]{TransportMode.pt, TransportMode.pt, TransportMode.car, TransportMode.car});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.pt, TransportMode.pt, TransportMode.pt, TransportMode.pt});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.pt, TransportMode.pt, TransportMode.pt, TransportMode.walk});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.pt, TransportMode.pt, TransportMode.bike, TransportMode.bike});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.pt, TransportMode.pt, TransportMode.walk, TransportMode.pt});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.pt, TransportMode.pt, TransportMode.walk, TransportMode.walk});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.pt, TransportMode.walk, TransportMode.car, TransportMode.car});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.pt, TransportMode.walk, TransportMode.pt, TransportMode.pt});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.pt, TransportMode.walk, TransportMode.pt, TransportMode.walk});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.pt, TransportMode.walk, TransportMode.bike, TransportMode.bike});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.pt, TransportMode.walk, TransportMode.walk, TransportMode.pt});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.pt, TransportMode.walk, TransportMode.walk, TransportMode.walk});

		expectedTourModeOptions.add(new TransportMode[]{TransportMode.bike, TransportMode.bike, TransportMode.car, TransportMode.car});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.bike, TransportMode.bike, TransportMode.pt, TransportMode.pt});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.bike, TransportMode.bike, TransportMode.pt, TransportMode.walk});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.bike, TransportMode.bike, TransportMode.bike, TransportMode.bike});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.bike, TransportMode.bike, TransportMode.walk, TransportMode.pt});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.bike, TransportMode.bike, TransportMode.walk, TransportMode.walk});

		expectedTourModeOptions.add(new TransportMode[]{TransportMode.walk, TransportMode.pt, TransportMode.car, TransportMode.car});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.walk, TransportMode.pt, TransportMode.pt, TransportMode.pt});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.walk, TransportMode.pt, TransportMode.pt, TransportMode.walk});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.walk, TransportMode.pt, TransportMode.bike, TransportMode.bike});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.walk, TransportMode.pt, TransportMode.walk, TransportMode.pt});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.walk, TransportMode.pt, TransportMode.walk, TransportMode.walk});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.walk, TransportMode.walk, TransportMode.car, TransportMode.car});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.walk, TransportMode.walk, TransportMode.pt, TransportMode.pt});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.walk, TransportMode.walk, TransportMode.pt, TransportMode.walk});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.walk, TransportMode.walk, TransportMode.bike, TransportMode.bike});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.walk, TransportMode.walk, TransportMode.walk, TransportMode.pt});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.walk, TransportMode.walk, TransportMode.walk, TransportMode.walk});
		testCases.put(testedActChainLocations, expectedTourModeOptions);

		////////////////////////////////////////////////////////////////
		//
		// 1 2 3 4
		//
		////////////////////////////////////////////////////////////////
		testedActChainLocations = "1 2 3 4";
		expectedTourModeOptions = new ArrayList<TransportMode[]>();
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.car, TransportMode.car, TransportMode.car});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.pt, TransportMode.pt, TransportMode.pt});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.pt, TransportMode.pt, TransportMode.walk});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.pt, TransportMode.walk, TransportMode.pt});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.pt, TransportMode.walk, TransportMode.walk});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.bike, TransportMode.bike, TransportMode.bike});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.walk, TransportMode.pt, TransportMode.pt});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.walk, TransportMode.pt, TransportMode.walk});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.walk, TransportMode.walk, TransportMode.pt});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.walk, TransportMode.walk, TransportMode.walk});
		testCases.put(testedActChainLocations, expectedTourModeOptions);

		////////////////////////////////////////////////////////////////
		//
		// 1 2 2 1
		//
		////////////////////////////////////////////////////////////////
		testedActChainLocations = "1 2 2 1";
		expectedTourModeOptions = new ArrayList<TransportMode[]>();

		expectedTourModeOptions.add(new TransportMode[]{TransportMode.car, TransportMode.car, TransportMode.car});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.car, TransportMode.pt, TransportMode.car});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.car, TransportMode.walk, TransportMode.car});

		expectedTourModeOptions.add(new TransportMode[]{TransportMode.pt, TransportMode.pt, TransportMode.pt});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.pt, TransportMode.pt, TransportMode.walk});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.pt, TransportMode.walk, TransportMode.pt});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.pt, TransportMode.walk, TransportMode.walk});

		expectedTourModeOptions.add(new TransportMode[]{TransportMode.bike, TransportMode.pt, TransportMode.bike});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.bike, TransportMode.bike, TransportMode.bike});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.bike, TransportMode.walk, TransportMode.bike});

		expectedTourModeOptions.add(new TransportMode[]{TransportMode.walk, TransportMode.pt, TransportMode.pt});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.walk, TransportMode.pt, TransportMode.walk});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.walk, TransportMode.walk, TransportMode.pt});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.walk, TransportMode.walk, TransportMode.walk});

		testCases.put(testedActChainLocations, expectedTourModeOptions);

		////////////////////////////////////////////////////////////////
		//
		// 1 2 3 4 3 2 1
		//
		////////////////////////////////////////////////////////////////
		testedActChainLocations = "1 2 3 4 3 2 1";
		expectedTourModeOptions = new ArrayList<TransportMode[]>();

		expectedTourModeOptions.add(new TransportMode[]{TransportMode.car, TransportMode.car, TransportMode.car, TransportMode.car, TransportMode.car, TransportMode.car});

		int variableLegs = 2;
		for (int ii = 0; ii < (int) Math.pow(2, variableLegs); ii++) {
			TransportMode[] combination = new TransportMode[testedActChainLocations.split(" ").length - 1];
			combination[0] = combination[1] = combination[4] = combination[5] = TransportMode.car;
			for (int jj = 0; jj < variableLegs ; jj++) {
				combination[jj + 2] = (((ii & ((int) Math.pow(2, variableLegs - (jj + 1)))) == 0)) ? TransportMode.pt : TransportMode.walk; 
			}
			expectedTourModeOptions.add(combination);
		}		

		variableLegs = 4;
		for (int ii = 0; ii < (int) Math.pow(2, variableLegs); ii++) {
			TransportMode[] combination = new TransportMode[testedActChainLocations.split(" ").length - 1];
			combination[0] = combination[5] = TransportMode.car;
			for (int jj = 0; jj < variableLegs ; jj++) {
				combination[jj + 1] = (((ii & ((int) Math.pow(2, variableLegs - (jj + 1)))) == 0)) ? TransportMode.pt : TransportMode.walk; 
			}
			expectedTourModeOptions.add(combination);
		}

		variableLegs = 5;
		for (int ii = 0; ii < (int) Math.pow(2, variableLegs); ii++) {
			TransportMode[] combination = new TransportMode[testedActChainLocations.split(" ").length - 1];
			combination[0] = TransportMode.pt;
			for (int jj = 0; jj < variableLegs; jj++) {
				combination[jj + 1] = (((ii & ((int) Math.pow(2, variableLegs - (jj + 1)))) == 0)) ? TransportMode.pt : TransportMode.walk; 
			}
			expectedTourModeOptions.add(combination);
		}

		variableLegs = 3;
		for (int ii = 0; ii < (int) Math.pow(2, variableLegs); ii++) {
			TransportMode[] combination = new TransportMode[testedActChainLocations.split(" ").length - 1];
			combination[0] = combination[5] = TransportMode.bike;
			combination[1] = TransportMode.pt;
			for (int jj = 0; jj < variableLegs; jj++) {
				combination[jj + 2] = (((ii & ((int) Math.pow(2, variableLegs - (jj + 1)))) == 0)) ? TransportMode.pt : TransportMode.walk; 
			}
			expectedTourModeOptions.add(combination);
		}

		expectedTourModeOptions.add(new TransportMode[]{TransportMode.bike, TransportMode.bike, TransportMode.pt, TransportMode.pt, TransportMode.bike, TransportMode.bike});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.bike, TransportMode.bike, TransportMode.pt, TransportMode.walk, TransportMode.bike, TransportMode.bike});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.bike, TransportMode.bike, TransportMode.bike, TransportMode.bike, TransportMode.bike, TransportMode.bike});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.bike, TransportMode.bike, TransportMode.walk, TransportMode.pt, TransportMode.bike, TransportMode.bike});
		expectedTourModeOptions.add(new TransportMode[]{TransportMode.bike, TransportMode.bike, TransportMode.walk, TransportMode.walk, TransportMode.bike, TransportMode.bike});

		variableLegs = 3;
		for (int ii = 0; ii < (int) Math.pow(2, variableLegs); ii++) {
			TransportMode[] combination = new TransportMode[testedActChainLocations.split(" ").length - 1];
			combination[0] = combination[5] = TransportMode.bike;
			combination[1] = TransportMode.walk;
			for (int jj = 0; jj < variableLegs; jj++) {
				combination[jj + 2] = (((ii & ((int) Math.pow(2, variableLegs - (jj + 1)))) == 0)) ? TransportMode.pt : TransportMode.walk; 
			}
			expectedTourModeOptions.add(combination);
		}

		variableLegs = 5;
		for (int ii = 0; ii < (int) Math.pow(2, variableLegs); ii++) {
			TransportMode[] combination = new TransportMode[testedActChainLocations.split(" ").length - 1];
			combination[0] = TransportMode.walk;
			for (int jj = 0; jj < variableLegs; jj++) {
				combination[jj + 1] = (((ii & ((int) Math.pow(2, variableLegs - (jj + 1)))) == 0)) ? TransportMode.pt : TransportMode.walk; 
			}
			expectedTourModeOptions.add(combination);
		}

		testCases.put(testedActChainLocations, expectedTourModeOptions);

		PlanAnalyzeTourModeChoiceSet testee = new PlanAnalyzeTourModeChoiceSet(new MeisterkConfigGroup().getChainBasedModes(), this.config.planomat().getTripStructureAnalysisLayer());
		EnumSet<TransportMode> possibleModes = EnumSet.of(TransportMode.walk, TransportMode.bike, TransportMode.pt, TransportMode.car);
		testee.setModeSet(possibleModes);

//		testee.setDoLogging(true);
		
		for (Entry<String, ArrayList<TransportMode[]>> entry : testCases.entrySet()) {

			String facString  = entry.getKey();
			log.info("Testing location sequence: " + facString);

			PlanImpl plan = this.generateTestPlan(facString, layer);
			
			testee.run(plan);

			ArrayList<TransportMode[]> actual = testee.getChoiceSet();
			assertEquals(entry.getValue().size(), actual.size());
			assertTrue(Arrays.deepEquals(
					(TransportMode[][]) entry.getValue().toArray(new TransportMode[0][0]), 
					(TransportMode[][]) actual.toArray(new TransportMode[0][0])));

		}

	}

	public void testIsModeChainFeasible() {
		
		// load data
		log.info("Reading network xml file...");
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(this.config.network().getInputFile());
		log.info("Reading network xml file...done.");

		// config
		this.config.planomat().setTripStructureAnalysisLayer(PlanomatConfigGroup.TripStructureAnalysisLayerOption.link);

		String facString = "1 2 3 4 3 2 1";
		PlanImpl testPlan = this.generateTestPlan(facString, network);
		EnumSet<TransportMode> chainBasedModes = EnumSet.of(TransportMode.bike, TransportMode.car);
		
		HashMap<TransportMode[], Boolean> candidates = new HashMap<TransportMode[], Boolean>();
		candidates.put(
				new TransportMode[]{
						TransportMode.car, 
						TransportMode.car, 
						TransportMode.car, 
						TransportMode.car, 
						TransportMode.car, 
						TransportMode.car}, 
				true);
		candidates.put(
				new TransportMode[]{
						TransportMode.car, 
						TransportMode.pt, 
						TransportMode.car, 
						TransportMode.car, 
						TransportMode.pt, 
						TransportMode.car}, 
				false);
		candidates.put(
				new TransportMode[]{
						TransportMode.car, 
						TransportMode.pt, 
						TransportMode.walk, 
						TransportMode.walk, 
						TransportMode.pt, 
						TransportMode.car}, 
				true);
		candidates.put(
				new TransportMode[]{
						TransportMode.car, 
						TransportMode.pt, 
						TransportMode.walk, 
						TransportMode.walk, 
						TransportMode.pt, 
						TransportMode.walk}, 
				false);

		for (TransportMode[] candidate : candidates.keySet()) {
			assertEquals(
					Boolean.valueOf(PlanAnalyzeTourModeChoiceSet.isModeChainFeasible(testPlan, candidate, chainBasedModes, this.config.planomat().getTripStructureAnalysisLayer())),
					candidates.get(candidate));
		}
		
	}
	
	private PlanImpl generateTestPlan(String facString, Layer layer) {

		PersonImpl person = new PersonImpl(new IdImpl("1000"));
		PlanomatConfigGroup.TripStructureAnalysisLayerOption tripStructureAnalysisLayer = this.config.planomat().getTripStructureAnalysisLayer();
		Location location = null;
		ActivityImpl act = null;
		
		PlanImpl plan = new org.matsim.core.population.PlanImpl(person);

		String[] locationIdSequence = facString.split(" ");
		for (int aa=0; aa < locationIdSequence.length; aa++) {
			location = layer.getLocation(new IdImpl(locationIdSequence[aa]));
			if (PlanomatConfigGroup.TripStructureAnalysisLayerOption.facility.equals(tripStructureAnalysisLayer)) {
				act = plan.createAndAddActivity("actAtFacility" + locationIdSequence[aa], (ActivityFacilityImpl) location);
			} else if (PlanomatConfigGroup.TripStructureAnalysisLayerOption.link.equals(tripStructureAnalysisLayer)) {
				act = plan.createAndAddActivity("actOnLink" + locationIdSequence[aa], (LinkImpl) location);
			}
			act.setEndTime(10*3600);
			if (aa != (locationIdSequence.length - 1)) {
				plan.createAndAddLeg(TransportMode.undefined);
			}
		}
		
		return plan;
		
	}
	
}
