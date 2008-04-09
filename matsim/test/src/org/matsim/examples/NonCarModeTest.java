/* *********************************************************************** *
 * project: org.matsim.*
 * NonCarModeTest.java
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

import org.matsim.config.Config;
import org.matsim.events.Events;
import org.matsim.events.algorithms.EventWriterTXT;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueSimulation;
import org.matsim.mobsim.SimulationTimer;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.CRCChecksum;
import org.matsim.world.World;


public class NonCarModeTest extends MatsimTestCase {

	public void testNonCarMode() {
		Config config = loadConfig(null);
		String netFileName = "testdata/studies/WIP/wip_net.xml";
		String popFileName = "testdata/studies/WIP/10plans_with_non_car_mode.xml";

		String eventsFileName = getOutputDirectory() + "berlinNonCarEvents.txt";
		String referenceFileName = "testdata/reference/berlinNonCarEvents.txt";

		Gbl.random.setSeed(7411L);

		World world = Gbl.getWorld();
		// this needs to be done before reading the network
		// because QueueLinks timeCap dependents on SIM_TICK_TIME_S
		SimulationTimer.reset(10);
		SimulationTimer.setTime(0);
		config.simulation().setFlowCapFactor(0.01);
		config.simulation().setStorageCapFactor(0.04);

		NetworkLayer network = new NetworkLayer() ;
		new MatsimNetworkReader(network).readFile(netFileName);
		world.setNetworkLayer(network);

		Plans population = new Plans();
		PlansReaderI plansReader = new MatsimPlansReader(population);
		plansReader.readFile(popFileName);
		world.setPopulation(population);

		Events events = new Events();
		EventWriterTXT writer = new EventWriterTXT(eventsFileName);
		events.addHandler(writer);
		world.setEvents(events);

		QueueSimulation sim = new QueueSimulation(network, population, events) ;
		sim.run();

		writer.closefile();

		final long checksum1 = CRCChecksum.getCRCFromFile(referenceFileName);
		long checksum2 = CRCChecksum.getCRCFromFile(eventsFileName);
		assertEquals(checksum1, checksum2);
		System.out.println("checksum = " + checksum2);

	}
}
