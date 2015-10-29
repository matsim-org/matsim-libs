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

package org.matsim.core.network;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.network.algorithms.NetworkCalcTopoType;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.NetworkMergeDoubleLinks;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.examples.TriangleScenario;
import org.matsim.testcases.MatsimTestCase;

/**
 *
 * @author balmermi
 */
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

	private final void runModules(final Network network) {
		log.info("  running network modules... ");
		new NetworkCleaner().run(network);
		new NetworkMergeDoubleLinks().run(network);
		new NetworkCalcTopoType().run(network);
		log.info("  done.");
	}

	private final void compareOutputNetwork() {
		log.info("  comparing input and output network file... ");
		long checksum_ref = CRCChecksum.getCRCFromFile(this.config.network().getInputFile());
		long checksum_run = CRCChecksum.getCRCFromFile(getOutputDirectory() + "output_network.xml");
		assertEquals(checksum_ref, checksum_run);
		log.info("  done.");
	}

	//////////////////////////////////////////////////////////////////////
	// tests
	//////////////////////////////////////////////////////////////////////

	public void testParserWriter_independentOfWorld() {
		log.info("running testParserWriter1()...");


		log.info("  reading network xml file independent of the world...");
		Scenario scenario = ScenarioUtils.createScenario(this.config);
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(this.config.network().getInputFile());
		log.info("  done.");

		this.runModules(network);

		TriangleScenario.writeNetwork(network, getOutputDirectory() + "output_network.xml");

		this.compareOutputNetwork();

		log.info("done.");
	}

	//////////////////////////////////////////////////////////////////////



	//////////////////////////////////////////////////////////////////////

	public void testParserWriter_withWorld_readNetworkFirst() {
		log.info("running testParserWriter4()...");

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(this.config);

		log.info("  reading network xml file as a layer of the world... ");
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(this.config.network().getInputFile());
		log.info("  done.");

		this.runModules(network);

		TriangleScenario.writeNetwork(network, getOutputDirectory() + "output_network.xml");

		this.compareOutputNetwork();

		log.info("done.");
	}

}
