/* *********************************************************************** *
 * project: org.matsim.*
 * ReRoutingTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.core.replanning;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
import org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType;
import org.matsim.core.config.groups.SimulationConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.core.utils.misc.PopulationUtils;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator;

import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;

public class ReRoutingTest extends MatsimTestCase {

	/*package*/ static final Logger log = Logger.getLogger(ReRoutingTest.class);

	
	private Scenario loadScenario() {
		Config config = loadConfig(getClassInputDirectory() + "config.xml");
		((SimulationConfigGroup) config.getModule("simulation")).setTimeStepSize(10.0);
		((SimulationConfigGroup) config.getModule("simulation")).setStuckTime(100.0);
		((SimulationConfigGroup) config.getModule("simulation")).setRemoveStuckVehicles(true);
		config.controler().setEventsFileFormats(EnumSet.of(EventsFileFormat.txt));

		/*
		 * The input plans file is not sorted. After switching from TreeMap to LinkedHashMap
		 * to store the persons in the population, we have to sort the population manually.  
		 * cdobler, oct'11
		 */
		Scenario scenario = ScenarioUtils.loadScenario(config);
		PopulationUtils.sortPersons(scenario.getPopulation());
		return scenario;
	}
	
	public void testReRoutingDijkstra() {
		Scenario scenario = this.loadScenario();
		scenario.getConfig().controler().setRoutingAlgorithmType(RoutingAlgorithmType.Dijkstra);
		TestControler controler = new TestControler(scenario);
		controler.setCreateGraphs(false);
		controler.setDumpDataAtEnd(false);
		controler.run();
		this.evaluate();
	}
	
	/**
	 * This test seems to have race conditions somewhere (i.e. it fails intermittently without code changes). kai, aug'13
	 */
	public void testReRouting() {
		Scenario scenario = this.loadScenario();
		scenario.getConfig().controler().setRoutingAlgorithmType(RoutingAlgorithmType.AStarLandmarks);
		TestControler controler = new TestControler(scenario);
		controler.setCreateGraphs(false);
		controler.setDumpDataAtEnd(false);
		controler.run();
		this.evaluate();
	}
		
	private void evaluate() {
		String inputEvents = getClassInputDirectory() + "0.events.txt.gz";
		String outputEvents = getOutputDirectory() + "ITERS/it.0/0.events.txt.gz";
		assertTrue("different event files", EventsFileComparator.compare(inputEvents, outputEvents) == EventsFileComparator.CODE_FILES_ARE_EQUAL);
		
		
		final String originalFileName = getInputDirectory() + "1.plans.xml.gz";
		long originalCheckSum = CRCChecksum.getCRCFromFile(originalFileName);
		final String revisedFileName = getOutputDirectory() + "ITERS/it.1/1.plans.xml.gz";
		long revisedCheckSum = CRCChecksum.getCRCFromFile(revisedFileName);
		
		if ( revisedCheckSum != originalCheckSum ) {

			List<String> original = fileToLines(originalFileName);
			List<String> revised  = fileToLines(revisedFileName);

			Patch patch = DiffUtils.diff(original, revised);

			for (Delta delta: patch.getDeltas()) {
				System.out.flush() ;
				System.err.println(delta.getOriginal());
				System.err.println(delta.getRevised());
				System.err.flush() ;
			}

		}
		assertEquals("different plans files", originalCheckSum, revisedCheckSum);
	}
	
	// Helper method for get the file content
	    private static List<String> fileToLines(String filename) {
	            List<String> lines = new LinkedList<String>();
	            String line = "";
	            try {
	                    BufferedReader in = IOUtils.getBufferedReader(filename);
	                    while ((line = in.readLine()) != null) {
	                            lines.add(line);
	                    }
	            } catch (IOException e) {
	                    e.printStackTrace();
	            }
	            return lines;
	    }


	static public class TestControler extends Controler {

		boolean mobsimRan = false;
		
		public TestControler(final Scenario scenario) {
			super(scenario);
		}

		@Override
		protected void setUp() {
			super.setUp();

			// do some test to ensure the scenario is correct
			int lastIter = this.config.controler().getLastIteration();
			if (lastIter < 1) {
				throw new IllegalArgumentException("Controler.lastIteration must be at least 1. Current value is " + lastIter);
			}
			if (lastIter > 1) {
				log.error("Controler.lastIteration is currently set to " + lastIter + ". Only the first iteration will be analyzed.");
			}
		}

		@Override
		protected void runMobSim() {
			if (!this.mobsimRan) {
				/* only run mobsim once, afterwards we're no longer interested
				 * in it as we have our plans-file to compare against to check the
				 * replanning.
				 */
				super.runMobSim();
				this.mobsimRan = true;
			} else {
				log.info("skipping mobsim, as it is not of interest in this iteration.");
			}
		}

	}
}
