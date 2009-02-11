/* *********************************************************************** *
 * project: org.matsim.*
 * MyControler1.java
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

package tutorial;

import org.matsim.config.Config;
import org.matsim.events.Events;
import org.matsim.events.algorithms.EventWriterTXT;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.queuesim.QueueSimulation;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Population;

public class MyControler1 {

	public static void main(final String[] args) {
		final String netFilename = "./examples/equil/network.xml";
		final String plansFilename = "./examples/equil/plans100.xml";

		@SuppressWarnings("unused")
		Config config = Gbl.createConfig(null);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);

		Population population = new Population();
		new MatsimPopulationReader(population, network).readFile(plansFilename);

		Events events = new Events();

		EventWriterTXT eventWriter = new EventWriterTXT("./output/events.txt");
		events.addHandler(eventWriter);

		QueueSimulation sim = new QueueSimulation(network, population, events);
		sim.run();

		eventWriter.closeFile();
	}

}
