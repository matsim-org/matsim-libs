/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesParserWriterTest.java
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

package org.matsim.facilities;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.examples.TriangleScenario;
import org.matsim.facilities.algorithms.FacilitiesCalcMinDist;
import org.matsim.facilities.algorithms.FacilitiesCombine;
import org.matsim.facilities.algorithms.FacilitiesSummary;
import org.matsim.testcases.MatsimTestCase;

public class FacilitiesParserWriterTest extends MatsimTestCase {

	private Config config = null;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.config = super.loadConfig(null);
		TriangleScenario.setUpScenarioConfig(this.config, super.getOutputDirectory());
	}

	@Override
	protected void tearDown() throws Exception {
		this.config = null;
		super.tearDown();
	}

	private void runModules(final ActivityFacilities facilities) {
		new FacilitiesSummary().run(facilities);
		new FacilitiesCalcMinDist().run(facilities);
		new FacilitiesCombine().run(facilities);
	}

	private void compareOutputFacilities(String filename) {
		long checksum_ref = CRCChecksum.getCRCFromFile(this.config.facilities().getInputFile());
		long checksum_run = CRCChecksum.getCRCFromFile(filename);
		assertEquals(checksum_ref, checksum_run);
	}

	public void testParserWriter1() {
		System.out.println("  reading facilites xml file independent of the world...");
		Scenario scenario = ScenarioUtils.createScenario(this.config);
		ActivityFacilities facilities = scenario.getActivityFacilities();
		new MatsimFacilitiesReader(scenario).readFile(this.config.facilities().getInputFile());

		this.runModules(facilities);

		TriangleScenario.writeFacilities(facilities, getOutputDirectory() + "output_facilities.xml");
	
		this.compareOutputFacilities(getOutputDirectory() + "output_facilities.xml");
	}

	//////////////////////////////////////////////////////////////////////

	public void testParserWriter2() {
		Scenario scenario = ScenarioUtils.createScenario(this.config);
	
		System.out.println("  reading facilites xml file as a layer of the world...");
		ActivityFacilities facilities = scenario.getActivityFacilities();
		new MatsimFacilitiesReader(scenario).readFile(this.config.facilities().getInputFile());

		this.runModules(facilities);

		TriangleScenario.writeFacilities(facilities, getOutputDirectory() + "output_facilities.xml");

		this.compareOutputFacilities(getOutputDirectory() + "output_facilities.xml");
	}

	//////////////////////////////////////////////////////////////////////

	public void testParserWriter3() {
		Scenario scenario = ScenarioUtils.createScenario(this.config);

		System.out.println("  reading facilites xml file as a layer of the world...");
		ActivityFacilities facilities = scenario.getActivityFacilities();
		new MatsimFacilitiesReader(scenario).readFile(this.config.facilities().getInputFile());

		this.runModules(facilities);

		TriangleScenario.writeFacilities(facilities, getOutputDirectory() + "output_facilities.xml");

		this.compareOutputFacilities(getOutputDirectory() + "output_facilities.xml");
	}

	//////////////////////////////////////////////////////////////////////

	public void testParserWriter4() {
		Scenario scenario = ScenarioUtils.createScenario(this.config);

		System.out.println("  reading facilites xml file as a layer of the world...");
		ActivityFacilities facilities = scenario.getActivityFacilities();
		new MatsimFacilitiesReader(scenario).readFile(this.config.facilities().getInputFile());

		this.runModules(facilities);

		TriangleScenario.writeFacilities(facilities, getOutputDirectory() + "output_facilities.xml");

		this.compareOutputFacilities(getOutputDirectory() + "output_facilities.xml");
	}
}
