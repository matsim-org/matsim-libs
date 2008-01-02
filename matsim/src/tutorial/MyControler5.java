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

import org.matsim.config.Config;
import org.matsim.events.Events;
import org.matsim.events.algorithms.EventWriterTXT;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.mobsim.QueueSimulation;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;
import org.matsim.plans.algorithms.PlanAverageScore;
import org.matsim.replanning.PlanStrategy;
import org.matsim.replanning.StrategyManager;
import org.matsim.replanning.modules.ReRoute;
import org.matsim.replanning.selectors.BestPlanSelector;
import org.matsim.replanning.selectors.RandomPlanSelector;
import org.matsim.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.scoring.CharyparNagelScoringFunctionFactory;
import org.matsim.scoring.EventsToScore;
import org.matsim.trafficmonitoring.TravelTimeCalculatorArray;
import org.matsim.utils.vis.netvis.NetVis;
import org.matsim.world.World;


public class MyControler5 {

	public static void main(final String[] args) {
		final String netFilename = "./examples/equil/network.xml";
		final String plansFilename = "./examples/equil/plans100.xml";

		@SuppressWarnings("unused")
		Config config = Gbl.createConfig(new String[] {"./examples/tutorial/myConfigScoring.xml"});

		World world = Gbl.getWorld();

		QueueNetworkLayer network = new QueueNetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);
		world.setNetworkLayer(network);

		Plans population = new Plans();
		PlansReaderI plansReader = new MatsimPlansReader(population);
		plansReader.readFile(plansFilename);
		world.setPopulation(population);

		Events events = new Events();

		EventWriterTXT eventWriter = new EventWriterTXT("./output/events.txt");
		events.addHandler(eventWriter);

		CharyparNagelScoringFunctionFactory factory = new CharyparNagelScoringFunctionFactory();
		EventsToScore scoring = new EventsToScore(population, factory);
		events.addHandler(scoring);

		StrategyManager strategyManager = new StrategyManager();
		PlanStrategy strategy1 = new PlanStrategy(new BestPlanSelector());
		PlanStrategy strategy2 = new PlanStrategy(new RandomPlanSelector());
		strategyManager.addStrategy(strategy1, 0.9);
		strategyManager.addStrategy(strategy2, 0.1);

		TravelTimeCalculatorArray ttimeCalc = new TravelTimeCalculatorArray(network);
		TravelTimeDistanceCostCalculator costCalc = new TravelTimeDistanceCostCalculator(ttimeCalc);
		strategy2.addStrategyModule(new ReRoute(network, costCalc, ttimeCalc));
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

		String[] visargs = {"./output/simout"};
		NetVis.main(visargs);
	}

}
