/* *********************************************************************** *
 * project: org.matsim.*
 * MyControler3.java
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

package tutorial.example3;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioLoader;
import org.matsim.vis.netvis.NetVis;

public class MyControler3 {

	public static void main(final String[] args) {
		final String netFilename = "./examples/equil/network.xml";
		final String plansFilename = "./examples/equil/plans100.xml";

		ScenarioLoader loader = new ScenarioLoader("./examples/tutorial/myConfig.xml");
		ScenarioImpl scenario = loader.getScenario();

		new MatsimNetworkReader(scenario.getNetwork()).readFile(netFilename);
		new MatsimPopulationReader(scenario).readFile(plansFilename);

		EventsManagerImpl events = new EventsManagerImpl();

//		EventWriterTXT eventWriter = new EventWriterTXT("./output/events.txt");
//		events.addHandler(eventWriter);

		QueueSimulation sim = new QueueSimulation(scenario, events);
		sim.openNetStateWriter("./output/simout", netFilename, 10);
		sim.run();

//		eventWriter.closeFile();

		String[] visargs = {"./output/simout"};
		NetVis.main(visargs);
	}

}
