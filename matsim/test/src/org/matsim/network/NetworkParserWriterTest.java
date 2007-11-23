/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkParserWriterTest.java
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

package org.matsim.network;

import org.matsim.examples.TriangleScenario;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.algorithms.NetworkCalcTopoType;
import org.matsim.network.algorithms.NetworkCleaner;
import org.matsim.network.algorithms.NetworkMergeDoubleLinks;
import org.matsim.network.algorithms.NetworkSummary;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.CRCChecksum;
import org.matsim.world.MatsimWorldReader;
import org.matsim.world.algorithms.WorldBottom2TopCompletion;
import org.matsim.world.algorithms.WorldCheck;
import org.matsim.world.algorithms.WorldValidation;

public class NetworkParserWriterTest extends MatsimTestCase {

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public NetworkParserWriterTest() {
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

	private final void runModules(NetworkLayer network) {
		System.out.println("  running network modules... ");
		new NetworkSummary().run(network);
		new NetworkCleaner(false).run(network);
		new NetworkMergeDoubleLinks().run(network);
		new NetworkCalcTopoType().run(network);
		new NetworkSummary().run(network);
		System.out.println("  done.");

		System.out.println("  running world modules... ");
		new WorldCheck().run(Gbl.getWorld());
		new WorldBottom2TopCompletion().run(Gbl.getWorld());
		new WorldValidation().run(Gbl.getWorld());
		new WorldCheck().run(Gbl.getWorld());
		System.out.println("  done.");
	}
	
	private final void compareOutputNetwork() {
		System.out.println("  comparing input and output network file... ");
		long checksum_ref = CRCChecksum.getCRCFromFile(Gbl.getConfig().network().getInputFile());
		long checksum_run = CRCChecksum.getCRCFromFile(Gbl.getConfig().network().getOutputFile());
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

		System.out.println("  reading network xml file independent of the world...");
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(Gbl.getConfig().network().getInputFile());
		System.out.println("  done.");

		this.runModules(network);

		TriangleScenario.writeConfig();
		TriangleScenario.writeNetwork(network);
		TriangleScenario.writeWorld();

		this.compareOutputNetwork();
		this.checkEmptyOutputWorld();
		
		System.out.println("done.");
	}
	
	//////////////////////////////////////////////////////////////////////

	public void testParserWriter2() {
		System.out.println("running testParserWriter2()...");

		System.out.println("  reading network xml file as a layer of the world...");
		NetworkLayer network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE,null);
		new MatsimNetworkReader(network).readFile(Gbl.getConfig().network().getInputFile());
		System.out.println("  done.");
		
		this.runModules(network);

		TriangleScenario.writeConfig();
		TriangleScenario.writeNetwork(network);
		TriangleScenario.writeWorld();
		
		this.compareOutputNetwork();
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

		System.out.println("  reading network xml file as a layer of the world... ");
		NetworkLayer network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE,null);
		new MatsimNetworkReader(network).readFile(Gbl.getConfig().network().getInputFile());
		System.out.println("  done.");
		
		this.runModules(network);

		TriangleScenario.writeConfig();
		TriangleScenario.writeNetwork(network);
		TriangleScenario.writeWorld();
		
		this.compareOutputNetwork();
		this.compareOutputWorld();

		System.out.println("done.");
	}

	//////////////////////////////////////////////////////////////////////

	public void testParserWriter4() {
		System.out.println("running testParserWriter4()...");

		System.out.println("  reading network xml file as a layer of the world... ");
		NetworkLayer network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE,null);
		new MatsimNetworkReader(network).readFile(Gbl.getConfig().network().getInputFile());
		System.out.println("  done.");
		
		this.runModules(network);

		System.out.println("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(Gbl.getWorld());
		worldReader.readFile(Gbl.getConfig().world().getInputFile());
		System.out.println("  done.");

		System.out.println("  running world modules... ");
		new WorldCheck().run(Gbl.getWorld());
		System.out.println("  done.");

		TriangleScenario.writeConfig();
		TriangleScenario.writeNetwork(network);
		TriangleScenario.writeWorld();
		
		this.compareOutputNetwork();
		this.compareOutputWorld();

		System.out.println("done.");
	}
}
