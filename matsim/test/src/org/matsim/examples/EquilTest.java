/* *********************************************************************** *
 * project: org.matsim.*
 * EquilTest.java
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

import org.matsim.events.Events;
import org.matsim.events.algorithms.EventWriterTXT;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.queuesim.QueueSimulation;
import org.matsim.mobsim.queuesim.SimulationTimer;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Population;
import org.matsim.population.PopulationReader;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.CRCChecksum;
import org.matsim.world.World;


public class EquilTest extends MatsimTestCase {

	public void testEquil() {
		loadConfig(null);
		String netFileName = "test/scenarios/equil/network.xml";
		String popFileName = "test/scenarios/equil/plans100.xml";

		String eventsFileName = getOutputDirectory() + "events.txt";
		String referenceFileName = getInputDirectory() + "events.txt.gz";

		World world = Gbl.createWorld();

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFileName);
		world.setNetworkLayer(network);
		world.complete();

		Population population = new Population();
		PopulationReader plansReader = new MatsimPopulationReader(population);
		plansReader.readFile(popFileName);

		Events events = new Events();
		EventWriterTXT writer = new EventWriterTXT(eventsFileName);
		events.addHandler(writer);

		SimulationTimer.setTime(0);
		QueueSimulation sim = new QueueSimulation(network, population, events);
		sim.run();

		writer.closefile();

		final long checksum1 = CRCChecksum.getCRCFromGZFile(referenceFileName);
		long checksum2 = CRCChecksum.getCRCFromFile(eventsFileName);
		assertEquals("different event files.", checksum1, checksum2);
	}
}
