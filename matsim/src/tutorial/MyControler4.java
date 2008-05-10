/* *********************************************************************** *
 * project: org.matsim.*
 * MyControler4.java
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
import org.matsim.mobsim.QueueSimulation;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Plans;
import org.matsim.plans.algorithms.PlanAverageScore;
import org.matsim.scoring.CharyparNagelScoringFunctionFactory;
import org.matsim.scoring.EventsToScore;
import org.matsim.utils.vis.netvis.NetVis;
import org.matsim.world.World;


public class MyControler4 {

	public static void main(final String[] args) {
		final String netFilename = "./examples/equil/network.xml";
		final String plansFilename = "./examples/equil/plans100.xml";

		@SuppressWarnings("unused")
		Config config = Gbl.createConfig(new String[] {"./examples/tutorial/myConfigScoring.xml"});

		World world = Gbl.getWorld();

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);
		world.setNetworkLayer(network);

		Plans population = new Plans();
		new MatsimPlansReader(population).readFile(plansFilename);

		Events events = new Events();

		EventWriterTXT eventWriter = new EventWriterTXT("./output/events.txt");
		events.addHandler(eventWriter);

		CharyparNagelScoringFunctionFactory factory = new CharyparNagelScoringFunctionFactory();
		EventsToScore scoring = new EventsToScore(population, factory);
		events.addHandler(scoring);

		QueueSimulation sim = new QueueSimulation(network, population, events);
		sim.openNetStateWriter("./output/simout", netFilename, 10);
		sim.run();

		scoring.finish();

		PlanAverageScore average = new PlanAverageScore();
		average.run(population);
		System.out.println("### the average score is: " + average.getAverage());

		eventWriter.closefile();

		String[] visargs = {"./output/simout"};
		NetVis.main(visargs);
	}

}
