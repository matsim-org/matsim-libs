/* *********************************************************************** *
 * project: org.matsim.*
 * GenerateEquilPopulationsTest.java
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

package playground.meisterk.phd;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.api.experimental.ScenarioLoader;
import org.matsim.core.config.Config;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestCase;

public class GenerateEquilPopulationsTest extends MatsimTestCase {

	private ScenarioImpl scenario = null;

	private enum InitialDemand {
		/**
		 * perform random initial demand generation wrt modes and times with planomat
		 */
		RANDOM,
		/**
		 * generate initial demand as in Bryan Raneys Ph.D. Thesis, equil test
		 */
		ALL6AM};

	private static final Logger logger = Logger.getLogger(GenerateEquilPopulationsTest.class);

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Config config = super.loadConfig(this.getClassInputDirectory() + "config.xml");
		this.scenario = new ScenarioImpl(config);
		ScenarioLoader loader = new ScenarioLoaderImpl(this.scenario);
		loader.loadScenario();
	}

	@Override
	protected void tearDown() throws Exception {
		this.scenario = null;
		super.tearDown();
	}

	public void testGenerateRandomCarOnly() {
		scenario.getConfig().planomat().setPossibleModes("car");
		this.runATest(InitialDemand.RANDOM);
	}

	public void testGenerateRandomCarPt() {
		scenario.getConfig().planomat().setPossibleModes("car,pt");
		this.runATest(InitialDemand.RANDOM);
	}

	public void testGenerateAll6AM() {

		this.runATest(InitialDemand.ALL6AM);

	}

	private void runATest(InitialDemand initialDemand) {

		GenerateEquilPopulations testee = new GenerateEquilPopulations();

		switch(initialDemand) {
		case RANDOM:
			testee.generateRandomInitialDemand(scenario);
			break;
		case ALL6AM:
			testee.generateAll6AMInitialDemand(scenario);
			break;
		}

		// write population out
		logger.info("Writing plans file...");
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).writeFile(this.getOutputDirectory() + "actual_plans.xml.gz");
		logger.info("Writing plans file...DONE.");

		// compare to expected population
		final long expectedChecksum = CRCChecksum.getCRCFromFile(this.getInputDirectory() + "expected_plans.xml.gz");
		final long actualChecksum = CRCChecksum.getCRCFromFile(this.getOutputDirectory() + "actual_plans.xml.gz");
		logger.info("Expected checksum: " + Long.toString(expectedChecksum));
		logger.info("Actual checksum: " + Long.toString(actualChecksum));
		assertEquals(expectedChecksum, actualChecksum);

	}

}
