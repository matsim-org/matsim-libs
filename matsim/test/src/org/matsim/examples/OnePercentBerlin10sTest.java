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
import org.matsim.core.config.Config;
import org.matsim.core.events.Events;
import org.matsim.core.events.algorithms.EventWriterTXT;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestCase;

public class OnePercentBerlin10sTest extends MatsimTestCase {

	private static final Logger log = Logger.getLogger(OnePercentBerlin10sTest.class);

	public void testOnePercent10s() {
		Config config = loadConfig(null);
		String netFileName = "test/scenarios/berlin/network.xml";
		String popFileName = "test/scenarios/berlin/plans_hwh_1pct.xml.gz";
		String eventsFileName = getOutputDirectory() + "events.txt";
		String referenceEventsFileName = getInputDirectory() + "events.txt.gz";

		MatsimRandom.reset(7411L);

		// this needs to be done before reading the network
		// because QueueLinks timeCap dependents on SIM_TICK_TIME_S
		config.simulation().setTimeStepSize(10.0);
		config.simulation().setFlowCapFactor(0.01);
		config.simulation().setStorageCapFactor(0.04);
		
		config.simulation().setRemoveStuckVehicles(false);
		config.simulation().setStuckTime(10.0);
		
		config.charyparNagelScoring().setLearningRate(1.0);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFileName);

		PopulationImpl population = new PopulationImpl();
		PopulationReader plansReader = new MatsimPopulationReader(population, network);
		plansReader.readFile(popFileName);
		population.printPlansCount();

		Events events = new Events();
		EventWriterTXT writer = new EventWriterTXT(eventsFileName);
		events.addHandler(writer);

		QueueSimulation sim = new QueueSimulation(network, population, events);
		log.info("START testOnePercent10s SIM");
		sim.run();
		log.info("STOP testOnePercent10s SIM");

		writer.closeFile();

		final long checksum1 = CRCChecksum.getCRCFromFile(referenceEventsFileName);
		final long checksum2 = CRCChecksum.getCRCFromFile(eventsFileName);
		assertEquals("different event files", checksum1, checksum2);
	}

}
