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

package tutorial.example4;

import org.matsim.core.api.Scenario;
import org.matsim.core.api.ScenarioLoader;
import org.matsim.core.config.Config;
import org.matsim.core.events.Events;
import org.matsim.core.events.algorithms.EventWriterTXT;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.charyparNagel.CharyparNagelScoringFunctionFactory;
import org.matsim.population.algorithms.PlanAverageScore;
import org.matsim.vis.netvis.NetVis;


public class MyControler4 {

	public static void main(final String[] args) {
		final String netFilename = "./examples/equil/network.xml";
		final String plansFilename = "./examples/equil/plans100.xml";

		ScenarioLoader loader = new ScenarioLoader("./examples/tutorial/myConfig.xml");
		Scenario scenario = loader.getScenario();
		Config config = scenario.getConfig();

		new MatsimNetworkReader(scenario.getNetwork()).readFile(netFilename);
		new MatsimPopulationReader(scenario).readFile(plansFilename);

		Events events = new Events();

		EventWriterTXT eventWriter = new EventWriterTXT("./output/events.txt");
		events.addHandler(eventWriter);

		CharyparNagelScoringFunctionFactory factory = new CharyparNagelScoringFunctionFactory(config.charyparNagelScoring());
		EventsToScore scoring = new EventsToScore(scenario.getPopulation(), factory);
		events.addHandler(scoring);

		QueueSimulation sim = new QueueSimulation(scenario, events);
		sim.openNetStateWriter("./output/simout", netFilename, 10);
		sim.run();

		scoring.finish();

		PlanAverageScore average = new PlanAverageScore();
		average.run(scenario.getPopulation());
		System.out.println("### the average score is: " + average.getAverage());

		eventWriter.closeFile();

		String[] visargs = {"./output/simout"};
		NetVis.main(visargs);
	}

}
