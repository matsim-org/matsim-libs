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

import org.apache.log4j.Logger;
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
import org.matsim.world.algorithms.WorldCheck;
import org.matsim.world.algorithms.WorldMappingInfo;

public class NetworkParserWriterTest extends MatsimTestCase {

	private Config config = null;
	private static final Logger log = Logger.getLogger(NetworkParserWriterTest.class);

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

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final void runModules(final NetworkLayer network, final World world) {
		log.info("  running network modules... ");
		new NetworkSummary().run(network);
		new NetworkCleaner().run(network);
		new NetworkMergeDoubleLinks().run(network);
		new NetworkCalcTopoType().run(network);
		new NetworkSummary().run(network);
		log.info("  done.");

		log.info("  running world modules... ");
		new WorldCheck().run(world);
		new WorldMappingInfo().run(world);
		log.info("  done.");
	}

	private final void compareOutputNetwork() {
		log.info("  comparing input and output network file... ");
		long checksum_ref = CRCChecksum.getCRCFromFile(this.config.network().getInputFile());
		long checksum_run = CRCChecksum.getCRCFromFile(this.config.network().getOutputFile());
		assertEquals(checksum_ref, checksum_run);
		log.info("  done.");
	}

	private final void compareOutputWorld() {
		log.info("  comparing input and output world file... ");
		long checksum_ref = CRCChecksum.getCRCFromFile(this.config.world().getInputFile());
		long checksum_run = CRCChecksum.getCRCFromFile(this.config.world().getOutputFile());
		assertEquals(checksum_ref, checksum_run);
		log.info("  done.");
	}

	private final void checkEmptyOutputWorld() {
		log.info("  checksum check of empty output world... ");
		long checksum_world = CRCChecksum.getCRCFromFile(this.config.world().getOutputFile());
		assertEquals(TriangleScenario.CHECKSUM_WORLD_EMPTY, checksum_world);
		log.info("  done.");
	}

	//////////////////////////////////////////////////////////////////////
	// tests
	//////////////////////////////////////////////////////////////////////

	public void testParserWriter1() {
		log.info("running testParserWriter1()...");

		final World world = new World();

		log.info("  reading network xml file independent of the world...");
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(this.config.network().getInputFile());
		world.complete();
		log.info("  done.");

		this.runModules(network, world);

		TriangleScenario.writeConfig(this.config);
		TriangleScenario.writeNetwork(network);
		TriangleScenario.writeWorld(world);

		this.compareOutputNetwork();
		this.checkEmptyOutputWorld();

		log.info("done.");
	}

	//////////////////////////////////////////////////////////////////////

	public void testParserWriter2() {
		log.info("running testParserWriter2()...");

		final World world = new World();

		log.info("  reading network xml file as a layer of the world...");
		NetworkLayer network = (NetworkLayer)world.createLayer(NetworkLayer.LAYER_TYPE,null);
		new MatsimNetworkReader(network).readFile(this.config.network().getInputFile());
		world.complete();
		log.info("  done.");

		this.runModules(network, world);

		TriangleScenario.writeConfig(this.config);
		TriangleScenario.writeNetwork(network);
		TriangleScenario.writeWorld(world);

		this.compareOutputNetwork();
		this.checkEmptyOutputWorld();

		log.info("done.");
	}

	//////////////////////////////////////////////////////////////////////

	public void testParserWriter3() {
		log.info("running testParserWriter3()...");

		final World world = new World();

		log.info("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(world);
		worldReader.readFile(this.config.world().getInputFile());
		world.complete();
		log.info("  done.");

		log.info("  running world modules... ");
		new WorldCheck().run(world);
		log.info("  done.");

		log.info("  reading network xml file as a layer of the world... ");
		NetworkLayer network = (NetworkLayer)world.createLayer(NetworkLayer.LAYER_TYPE,null);
		new MatsimNetworkReader(network).readFile(this.config.network().getInputFile());
		world.complete();
		log.info("  done.");

		this.runModules(network, world);

		TriangleScenario.writeConfig(this.config);
		TriangleScenario.writeNetwork(network);
		TriangleScenario.writeWorld(world);

		this.compareOutputNetwork();
		this.compareOutputWorld();

		log.info("done.");
	}

	//////////////////////////////////////////////////////////////////////

	public void testParserWriter4() {
		log.info("running testParserWriter4()...");

		final World world = new World();

		log.info("  reading network xml file as a layer of the world... ");
		NetworkLayer network = (NetworkLayer)world.createLayer(NetworkLayer.LAYER_TYPE,null);
		new MatsimNetworkReader(network).readFile(this.config.network().getInputFile());
		world.complete();
		log.info("  done.");

		this.runModules(network, world);

		log.info("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(world);
		worldReader.readFile(this.config.world().getInputFile());
		world.complete();
		log.info("  done.");

		log.info("  running world modules... ");
		new WorldCheck().run(world);
		log.info("  done.");

		TriangleScenario.writeConfig(this.config);
		TriangleScenario.writeNetwork(network);
		TriangleScenario.writeWorld(world);

		this.compareOutputNetwork();
		this.compareOutputWorld();

		log.info("done.");
	}
}
