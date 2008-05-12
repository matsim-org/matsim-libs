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

import org.matsim.config.Config;
import org.matsim.examples.TriangleScenario;
import org.matsim.network.algorithms.NetworkCalcTopoType;
import org.matsim.network.algorithms.NetworkCleaner;
import org.matsim.network.algorithms.NetworkMergeDoubleLinks;
import org.matsim.network.algorithms.NetworkSummary;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.CRCChecksum;
import org.matsim.world.MatsimWorldReader;
import org.matsim.world.World;
import org.matsim.world.algorithms.WorldBottom2TopCompletion;
import org.matsim.world.algorithms.WorldCheck;
import org.matsim.world.algorithms.WorldValidation;

public class NetworkParserWriterTest extends MatsimTestCase {

	private Config config = null;
	
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
		this.config = super.loadConfig(null);
		TriangleScenario.setUpScenarioConfig(this.config, super.getOutputDirectory());
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final void runModules(final NetworkLayer network, final World world) {
		System.out.println("  running network modules... ");
		new NetworkSummary().run(network);
		new NetworkCleaner().run(network);
		new NetworkMergeDoubleLinks().run(network);
		new NetworkCalcTopoType().run(network);
		new NetworkSummary().run(network);
		System.out.println("  done.");

		System.out.println("  running world modules... ");
		new WorldCheck().run(world);
		new WorldBottom2TopCompletion().run(world);
		new WorldValidation().run(world);
		new WorldCheck().run(world);
		System.out.println("  done.");
	}
	
	private final void compareOutputNetwork() {
		System.out.println("  comparing input and output network file... ");
		long checksum_ref = CRCChecksum.getCRCFromFile(this.config.network().getInputFile());
		long checksum_run = CRCChecksum.getCRCFromFile(this.config.network().getOutputFile());
		assertEquals(checksum_ref, checksum_run);
		System.out.println("  done.");
	}

	private final void compareOutputWorld() {
		System.out.println("  comparing input and output world file... ");
		long checksum_ref = CRCChecksum.getCRCFromFile(this.config.world().getInputFile());
		long checksum_run = CRCChecksum.getCRCFromFile(this.config.world().getOutputFile());
		assertEquals(checksum_ref, checksum_run);
		System.out.println("  done.");
	}

	private final void checkEmptyOutputWorld() {
		System.out.println("  checksum check of empty output world... ");
		long checksum_world = CRCChecksum.getCRCFromFile(this.config.world().getOutputFile());
		assertEquals(TriangleScenario.CHECKSUM_WORLD_EMPTY, checksum_world);
		System.out.println("  done.");
	}
	
	//////////////////////////////////////////////////////////////////////
	// tests
	//////////////////////////////////////////////////////////////////////

	public void testParserWriter1() {
		System.out.println("running testParserWriter1()...");

		final World world = new World();
		
		System.out.println("  reading network xml file independent of the world...");
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(this.config.network().getInputFile());
		System.out.println("  done.");

		this.runModules(network, world);

		TriangleScenario.writeConfig(this.config);
		TriangleScenario.writeNetwork(network);
		TriangleScenario.writeWorld(world);

		this.compareOutputNetwork();
		this.checkEmptyOutputWorld();
		
		System.out.println("done.");
	}
	
	//////////////////////////////////////////////////////////////////////

	public void testParserWriter2() {
		System.out.println("running testParserWriter2()...");

		final World world = new World();
		
		System.out.println("  reading network xml file as a layer of the world...");
		NetworkLayer network = (NetworkLayer)world.createLayer(NetworkLayer.LAYER_TYPE,null);
		new MatsimNetworkReader(network).readFile(this.config.network().getInputFile());
		System.out.println("  done.");
		
		this.runModules(network, world);

		TriangleScenario.writeConfig(this.config);
		TriangleScenario.writeNetwork(network);
		TriangleScenario.writeWorld(world);
		
		this.compareOutputNetwork();
		this.checkEmptyOutputWorld();

		System.out.println("done.");
	}

	//////////////////////////////////////////////////////////////////////

	public void testParserWriter3() {
		System.out.println("running testParserWriter3()...");

		final World world = new World();
		
		System.out.println("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(world);
		worldReader.readFile(this.config.world().getInputFile());
		System.out.println("  done.");

		System.out.println("  running world modules... ");
		new WorldCheck().run(world);
		System.out.println("  done.");

		System.out.println("  reading network xml file as a layer of the world... ");
		NetworkLayer network = (NetworkLayer)world.createLayer(NetworkLayer.LAYER_TYPE,null);
		new MatsimNetworkReader(network).readFile(this.config.network().getInputFile());
		System.out.println("  done.");
		
		this.runModules(network, world);

		TriangleScenario.writeConfig(this.config);
		TriangleScenario.writeNetwork(network);
		TriangleScenario.writeWorld(world);
		
		this.compareOutputNetwork();
		this.compareOutputWorld();

		System.out.println("done.");
	}

	//////////////////////////////////////////////////////////////////////

	public void testParserWriter4() {
		System.out.println("running testParserWriter4()...");

		final World world = new World();
		
		System.out.println("  reading network xml file as a layer of the world... ");
		NetworkLayer network = (NetworkLayer)world.createLayer(NetworkLayer.LAYER_TYPE,null);
		new MatsimNetworkReader(network).readFile(this.config.network().getInputFile());
		System.out.println("  done.");
		
		this.runModules(network, world);

		System.out.println("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(world);
		worldReader.readFile(this.config.world().getInputFile());
		System.out.println("  done.");

		System.out.println("  running world modules... ");
		new WorldCheck().run(world);
		System.out.println("  done.");

		TriangleScenario.writeConfig(this.config);
		TriangleScenario.writeNetwork(network);
		TriangleScenario.writeWorld(world);
		
		this.compareOutputNetwork();
		this.compareOutputWorld();

		System.out.println("done.");
	}
}
