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

package org.matsim.core.facilities;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.Config;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.examples.TriangleScenario;
import org.matsim.facilities.algorithms.FacilitiesCalcMinDist;
import org.matsim.facilities.algorithms.FacilitiesCombine;
import org.matsim.facilities.algorithms.FacilitiesSummary;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.world.MatsimWorldReader;
import org.matsim.world.World;

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

	private final void runModules(final ActivityFacilitiesImpl facilities, final World world) {
		System.out.println("  running facilities modules... ");
		new FacilitiesSummary().run(facilities);
		new FacilitiesCalcMinDist().run(facilities);
		new FacilitiesCombine().run(facilities);
		System.out.println("  done.");
	}

	private final void compareOutputFacilities(String filename) {
		System.out.println("  comparing input and output facilities file... ");
		long checksum_ref = CRCChecksum.getCRCFromFile(this.config.facilities().getInputFile());
		long checksum_run = CRCChecksum.getCRCFromFile(filename);
		assertEquals(checksum_ref, checksum_run);
		System.out.println("  done.");
	}

	private final void compareOutputWorld(String refFilename, String testFilename) {
		System.out.println("  comparing input and output world file... ");
		long checksum_ref = CRCChecksum.getCRCFromFile(refFilename);
		long checksum_run = CRCChecksum.getCRCFromFile(testFilename);
		assertEquals(checksum_ref, checksum_run);
		System.out.println("  done.");
	}

	private final void checkEmptyOutputWorld(String filename) {
		System.out.println("  checksum check of empty output world... ");
		long checksum_world = CRCChecksum.getCRCFromFile(filename);
		assertEquals(TriangleScenario.CHECKSUM_WORLD_EMPTY,checksum_world);
		System.out.println("  done.");
	}

	//////////////////////////////////////////////////////////////////////
	// tests
	//////////////////////////////////////////////////////////////////////

	public void testParserWriter1() {
		System.out.println("running testParserWriter1()...");

		final World world = new World();

		System.out.println("  reading facilites xml file independent of the world...");
		ScenarioImpl scenario = new ScenarioImpl(this.config);
		ActivityFacilitiesImpl facilities = scenario.getActivityFacilities();
		new MatsimFacilitiesReader(scenario).readFile(this.config.facilities().getInputFile());
		world.complete(this.config);
		System.out.println("  done.");

		this.runModules(facilities, world);

		TriangleScenario.writeFacilities(facilities, getOutputDirectory() + "output_facilities.xml");
		TriangleScenario.writeWorld(world, getOutputDirectory() + "output_world.xml");

		this.compareOutputFacilities(getOutputDirectory() + "output_facilities.xml");
		this.checkEmptyOutputWorld(getOutputDirectory() + "output_world.xml");

		System.out.println("done.");
	}

	//////////////////////////////////////////////////////////////////////

	public void testParserWriter2() {
		System.out.println("running testParserWriter2()...");

		ScenarioImpl scenario = new ScenarioImpl(this.config);
		final World world = scenario.getWorld();

		System.out.println("  reading facilites xml file as a layer of the world...");
		ActivityFacilitiesImpl facilities = scenario.getActivityFacilities();
		new MatsimFacilitiesReader(scenario).readFile(this.config.facilities().getInputFile());
		world.complete(this.config);
		System.out.println("  done.");

		this.runModules(facilities, world);

		TriangleScenario.writeFacilities(facilities, getOutputDirectory() + "output_facilities.xml");
		TriangleScenario.writeWorld(world, getOutputDirectory() + "output_world.xml");

		this.compareOutputFacilities(getOutputDirectory() + "output_facilities.xml");
		this.checkEmptyOutputWorld(getOutputDirectory() + "output_world.xml");

		System.out.println("done.");
	}

	//////////////////////////////////////////////////////////////////////

	public void testParserWriter3() {
		System.out.println("running testParserWriter3()...");

		ScenarioImpl scenario = new ScenarioImpl(this.config);
		System.out.println("  reading world xml file... ");
		final World world = scenario.getWorld();
		final MatsimWorldReader worldReader = new MatsimWorldReader(scenario);
		worldReader.readFile("test/scenarios/triangle/world.xml");
		world.complete(this.config);
		System.out.println("  done.");

		System.out.println("  reading facilites xml file as a layer of the world...");
		ActivityFacilitiesImpl facilities = scenario.getActivityFacilities();
		new MatsimFacilitiesReader(scenario).readFile(this.config.facilities().getInputFile());
		world.complete(this.config);
		System.out.println("  done.");

		this.runModules(facilities, world);

		TriangleScenario.writeFacilities(facilities, getOutputDirectory() + "output_facilities.xml");
		TriangleScenario.writeWorld(world, getOutputDirectory() + "output_world.xml");

		this.compareOutputFacilities(getOutputDirectory() + "output_facilities.xml");
		this.compareOutputWorld("test/scenarios/triangle/world.xml", getOutputDirectory() + "output_world.xml");

		System.out.println("done.");
	}

	//////////////////////////////////////////////////////////////////////

	public void testParserWriter4() {
		System.out.println("running testParserWriter4()...");

		ScenarioImpl scenario = new ScenarioImpl(this.config);
		final World world = scenario.getWorld();

		System.out.println("  reading facilites xml file as a layer of the world...");
		ActivityFacilitiesImpl facilities = scenario.getActivityFacilities();
		new MatsimFacilitiesReader(scenario).readFile(this.config.facilities().getInputFile());
		world.complete(this.config);
		System.out.println("  done.");

		this.runModules(facilities, world);

		System.out.println("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(scenario);
		worldReader.readFile("test/scenarios/triangle/world.xml");
		world.complete(this.config);
		System.out.println("  done.");

		TriangleScenario.writeFacilities(facilities, getOutputDirectory() + "output_facilities.xml");
		TriangleScenario.writeWorld(world, getOutputDirectory() + "output_world.xml");

		this.compareOutputFacilities(getOutputDirectory() + "output_facilities.xml");
		this.compareOutputWorld("test/scenarios/triangle/world.xml", getOutputDirectory() + "output_world.xml");

		System.out.println("done.");
	}
}
