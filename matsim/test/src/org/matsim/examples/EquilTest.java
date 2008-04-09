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
import org.matsim.events.algorithms.EventWriterXML;
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


public class EquilTest extends MatsimTestCase {

	public void testEquil() {
		loadConfig(null);
		String netFileName = "test/scenarios/equil/network.xml";
		String popFileName = "test/scenarios/equil/plans100.xml";

		String eventsFileName = getOutputDirectory() + "eventsFile.xml";
		String referenceFileName = getInputDirectory() + "events.xml.gz";

		World world = Gbl.getWorld();

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFileName);
		world.setNetworkLayer(network);

		Plans population = new Plans();
		PlansReaderI plansReader = new MatsimPlansReader(population);
		plansReader.readFile(popFileName);
		world.setPopulation(population);

		Events events = new Events();
		EventWriterXML writer = new EventWriterXML(eventsFileName);
		events.addHandler(writer);
		world.setEvents(events);

		SimulationTimer.setTime(0);
		QueueSimulation sim = new QueueSimulation(network, population, events) ;
		sim.run();

		writer.closefile();

		final long checksum1 = CRCChecksum.getCRCFromGZFile(referenceFileName);
		long checksum2 = CRCChecksum.getCRCFromFile(eventsFileName);
		System.out.println("checksum1 = " + checksum1);
		System.out.println("checksum2 = " + checksum2);
		assertEquals(checksum1, checksum2);

	}
}
