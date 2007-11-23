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

import org.matsim.examples.TriangleScenario;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.facilities.algorithms.FacilitiesCalcMinDist;
import org.matsim.facilities.algorithms.FacilitiesCombine;
import org.matsim.facilities.algorithms.FacilitiesSummary;
import org.matsim.gbl.Gbl;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.CRCChecksum;
import org.matsim.world.MatsimWorldReader;
import org.matsim.world.algorithms.WorldBottom2TopCompletion;
import org.matsim.world.algorithms.WorldCheck;
import org.matsim.world.algorithms.WorldValidation;

public class FacilitiesParserWriterTest extends MatsimTestCase {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public FacilitiesParserWriterTest() {
	}

	//////////////////////////////////////////////////////////////////////
	// setup
	//////////////////////////////////////////////////////////////////////

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		super.loadConfig(null);
		TriangleScenario.setUpScenarioConfig(super.getOutputDirectory());
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final void runModules(Facilities facilities) {
		System.out.println("  running facilities modules... ");
		new FacilitiesSummary().run(facilities);
		new FacilitiesCalcMinDist().run(facilities);
		new FacilitiesCombine().run(facilities);
		System.out.println("  done.");

		System.out.println("  running world modules... ");
		new WorldCheck().run(Gbl.getWorld());
		new WorldBottom2TopCompletion().run(Gbl.getWorld());
		new WorldValidation().run(Gbl.getWorld());
		new WorldCheck().run(Gbl.getWorld());
		System.out.println("  done.");
	}
	
	private final void compareOutputFacilities() {
		System.out.println("  comparing input and output facilities file... ");
		long checksum_ref = CRCChecksum.getCRCFromFile(Gbl.getConfig().facilities().getInputFile());
		long checksum_run = CRCChecksum.getCRCFromFile(Gbl.getConfig().facilities().getOutputFile());
		assertEquals(checksum_ref, checksum_run);
		System.out.println("  done.");
	}

	private final void compareOutputWorld() {
		System.out.println("  comparing input and output world file... ");
		long checksum_ref = CRCChecksum.getCRCFromFile(Gbl.getConfig().world().getInputFile());
		long checksum_run = CRCChecksum.getCRCFromFile(Gbl.getConfig().world().getOutputFile());
		assertEquals(checksum_ref, checksum_run);
		System.out.println("  done.");
	}

	private final void checkEmptyOutputWorld() {
		System.out.println("  checksum check of empty output world... ");
		long checksum_world = CRCChecksum.getCRCFromFile(Gbl.getConfig().world().getOutputFile());
		assertEquals(TriangleScenario.CHECKSUM_WORLD_EMPTY,checksum_world);
		System.out.println("  done.");
	}
	
	//////////////////////////////////////////////////////////////////////
	// tests
	//////////////////////////////////////////////////////////////////////

	public void testParserWriter1() {
		System.out.println("running testParserWriter1()...");

		System.out.println("  reading facilites xml file independent of the world...");
		Facilities facilities = new Facilities();
		new MatsimFacilitiesReader(facilities).readFile(Gbl.getConfig().facilities().getInputFile());
		System.out.println("  done.");

		this.runModules(facilities);

		TriangleScenario.writeConfig();
		TriangleScenario.writeFacilities(facilities);
		TriangleScenario.writeWorld();

		this.compareOutputFacilities();
		this.checkEmptyOutputWorld();
		
		System.out.println("done.");
	}

	//////////////////////////////////////////////////////////////////////

	public void testParserWriter2() {
		System.out.println("running testParserWriter2()...");

		System.out.println("  reading facilites xml file as a layer of the world...");
		Facilities facilities = (Facilities)Gbl.getWorld().createLayer(Facilities.LAYER_TYPE,null);
		new MatsimFacilitiesReader(facilities).readFile(Gbl.getConfig().facilities().getInputFile());
		System.out.println("  done.");

		this.runModules(facilities);

		TriangleScenario.writeConfig();
		TriangleScenario.writeFacilities(facilities);
		TriangleScenario.writeWorld();

		this.compareOutputFacilities();
		this.checkEmptyOutputWorld();
		
		System.out.println("done.");
	}

	//////////////////////////////////////////////////////////////////////

	public void testParserWriter3() {
		System.out.println("running testParserWriter3()...");

		System.out.println("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(Gbl.getWorld());
		worldReader.readFile(Gbl.getConfig().world().getInputFile());
		System.out.println("  done.");

		System.out.println("  running world modules... ");
		new WorldCheck().run(Gbl.getWorld());
		System.out.println("  done.");

		System.out.println("  reading facilites xml file as a layer of the world...");
		Facilities facilities = (Facilities)Gbl.getWorld().createLayer(Facilities.LAYER_TYPE,null);
		new MatsimFacilitiesReader(facilities).readFile(Gbl.getConfig().facilities().getInputFile());
		System.out.println("  done.");

		this.runModules(facilities);

		TriangleScenario.writeConfig();
		TriangleScenario.writeFacilities(facilities);
		TriangleScenario.writeWorld();

		this.compareOutputFacilities();
		this.compareOutputWorld();
		
		System.out.println("done.");
	}

	//////////////////////////////////////////////////////////////////////

	public void testParserWriter4() {
		System.out.println("running testParserWriter4()...");

		System.out.println("  reading facilites xml file as a layer of the world...");
		Facilities facilities = (Facilities)Gbl.getWorld().createLayer(Facilities.LAYER_TYPE,null);
		new MatsimFacilitiesReader(facilities).readFile(Gbl.getConfig().facilities().getInputFile());
		System.out.println("  done.");

		this.runModules(facilities);

		System.out.println("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(Gbl.getWorld());
		worldReader.readFile(Gbl.getConfig().world().getInputFile());
		System.out.println("  done.");

		System.out.println("  running world modules... ");
		new WorldCheck().run(Gbl.getWorld());
		System.out.println("  done.");

		TriangleScenario.writeConfig();
		TriangleScenario.writeFacilities(facilities);
		TriangleScenario.writeWorld();

		this.compareOutputFacilities();
		this.compareOutputWorld();
		
		System.out.println("done.");
	}
}
