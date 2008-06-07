/* *********************************************************************** *
 * project: org.matsim.*
 * OnePercentBerlin10sTest.java
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

package org.matsim.examples;

import org.apache.log4j.Logger;
import org.matsim.config.Config;
import org.matsim.events.Events;
import org.matsim.events.algorithms.EventWriterTXT;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueSimulation;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.CRCChecksum;
import org.matsim.world.World;

public class OnePercentBerlin10sTest extends MatsimTestCase {

	private static final Logger log = Logger.getLogger(OnePercentBerlin10sTest.class);

	public void testOnePercent10s() {
		Config config = loadConfig(null);
		String netFileName = "test/scenarios/berlin/network.xml";
		String popFileName = "test/scenarios/berlin/plans_hwh_1pct.xml.gz";
		String eventsFileName = getOutputDirectory() + "events.txt";
		String referenceFileName = getInputDirectory() + "events.txt.gz";

		Gbl.random.setSeed(7411L);

		World world = Gbl.createWorld();
		// this needs to be done before reading the network
		// because QueueLinks timeCap dependents on SIM_TICK_TIME_S
		config.simulation().setTimeStepSize(10.0);
		config.simulation().setFlowCapFactor(0.01);
		config.simulation().setStorageCapFactor(0.04);
		config.charyparNagelScoring().setLearningRate(1.0);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFileName);
		world.setNetworkLayer(network);

		Plans population = new Plans(Plans.NO_STREAMING);
		PlansReaderI plansReader = new MatsimPlansReader(population);
		plansReader.readFile(popFileName);
		population.printPlansCount();

		Events events = new Events();
		EventWriterTXT writer = new EventWriterTXT(eventsFileName);
		events.addHandler(writer);

		QueueSimulation sim = new QueueSimulation(network, population, events);
		log.info("START testOnePercent10s SIM");
		sim.run();
		log.info("STOP testOnePercent10s SIM");

		writer.closefile();

		log.info("calculating checksums...");
		final long checksum1 = CRCChecksum.getCRCFromGZFile(referenceFileName);
		final long checksum2 = CRCChecksum.getCRCFromFile(eventsFileName);
		log.info("checksum = " + checksum2 + " should be: " + checksum1);
		assertEquals(checksum1, checksum2);
	}

}
