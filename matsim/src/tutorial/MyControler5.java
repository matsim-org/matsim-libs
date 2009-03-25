/* *********************************************************************** *
 * project: org.matsim.*
 * MyControler5.java
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

import org.matsim.core.api.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.events.Events;
import org.matsim.core.events.algorithms.EventWriterTXT;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.modules.ReRouteDijkstra;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.core.scoring.CharyparNagelScoringFunctionFactory;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.population.algorithms.PlanAverageScore;
import org.matsim.vis.netvis.NetVis;


public class MyControler5 {

	public static void main(final String[] args) {
		final String netFilename = "./examples/equil/network.xml";
		final String plansFilename = "./examples/equil/plans100.xml";

		@SuppressWarnings("unused")
		Config config = Gbl.createConfig(new String[] {"./examples/tutorial/myConfigScoring.xml"});

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);

		Population population = new PopulationImpl();
		new MatsimPopulationReader(population, network).readFile(plansFilename);

		Events events = new Events();

		EventWriterTXT eventWriter = new EventWriterTXT("./output/events.txt");
		events.addHandler(eventWriter);

		CharyparNagelScoringFunctionFactory factory = new CharyparNagelScoringFunctionFactory(config.charyparNagelScoring());
		EventsToScore scoring = new EventsToScore(population, factory);
		events.addHandler(scoring);

		StrategyManager strategyManager = new StrategyManager();
		PlanStrategy strategy1 = new PlanStrategy(new BestPlanSelector());
		PlanStrategy strategy2 = new PlanStrategy(new RandomPlanSelector());
		strategyManager.addStrategy(strategy1, 0.9);
		strategyManager.addStrategy(strategy2, 0.1);

		TravelTimeCalculator ttimeCalc = new TravelTimeCalculator(network);
		TravelTimeDistanceCostCalculator costCalc = new TravelTimeDistanceCostCalculator(ttimeCalc);
		strategy2.addStrategyModule(new ReRouteDijkstra(network, costCalc, ttimeCalc));
		events.addHandler(ttimeCalc);

		for (int iteration = 0; iteration <= 10; iteration++) {
			events.resetHandlers(iteration);
			eventWriter.init("./output/events.txt");

			QueueSimulation sim = new QueueSimulation(network, population, events);
			sim.openNetStateWriter("./output/simout", netFilename, 10);
			sim.run();

			scoring.finish();

			PlanAverageScore average = new PlanAverageScore();
			average.run(population);
			System.out.println("### the average score in iteration " + iteration + " is: " + average.getAverage());

			strategyManager.run(population);
		}

		Gbl.setConfig(null);
		String[] visargs = {"./output/simout"};
		NetVis.main(visargs);
	}

}
