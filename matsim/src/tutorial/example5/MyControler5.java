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

package tutorial.example5;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioLoader;
import org.matsim.core.config.Config;
import org.matsim.core.events.Events;
import org.matsim.core.events.algorithms.EventWriterTXT;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.modules.ReRouteDijkstra;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.charyparNagel.CharyparNagelScoringFunctionFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.population.algorithms.PlanAverageScore;
import org.matsim.vis.netvis.NetVis;


public class MyControler5 {

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

		StrategyManager strategyManager = new StrategyManager();
		PlanStrategy strategy1 = new PlanStrategy(new BestPlanSelector());
		PlanStrategy strategy2 = new PlanStrategy(new RandomPlanSelector());
		strategyManager.addStrategy(strategy1, 0.9);
		strategyManager.addStrategy(strategy2, 0.1);

		TravelTimeCalculator ttimeCalc = new TravelTimeCalculator(scenario.getNetwork(), scenario.getConfig().travelTimeCalculator());
		TravelTimeDistanceCostCalculator costCalc = new TravelTimeDistanceCostCalculator(ttimeCalc, config.charyparNagelScoring());
		strategy2.addStrategyModule(new ReRouteDijkstra(config.plansCalcRoute(), scenario.getNetwork(), costCalc, ttimeCalc));
		events.addHandler(ttimeCalc);

		for (int iteration = 0; iteration <= 10; iteration++) {
			events.resetHandlers(iteration);
			eventWriter.init("./output/events.txt");

			QueueSimulation sim = new QueueSimulation(scenario, events);
			sim.openNetStateWriter("./output/simout", netFilename, 10);
			sim.run();

			scoring.finish();

			PlanAverageScore average = new PlanAverageScore();
			average.run(scenario.getPopulation());
			System.out.println("### the average score in iteration " + iteration + " is: " + average.getAverage());

			strategyManager.run(scenario.getPopulation());
		}

		String[] visargs = {"./output/simout"};
		NetVis.main(visargs);
	}

}
