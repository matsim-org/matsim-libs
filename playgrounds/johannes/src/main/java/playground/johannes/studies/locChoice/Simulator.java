/* *********************************************************************** *
 * project: org.matsim.*
 * Simulator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.studies.locChoice;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.selectors.KeepSelected;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.charyparNagel.CharyparNagelScoringFunctionFactory;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.sim.analysis.NearestLocation;
import playground.johannes.socialnetworks.sim.analysis.PlanAnalyzerTaskComposite;
import playground.johannes.socialnetworks.sim.analysis.PlansAnalyzer;
import playground.johannes.socialnetworks.sim.analysis.TravelDistanceTask;
import playground.johannes.socialnetworks.sim.interaction.PseudoSim;
import playground.johannes.socialnetworks.sim.locationChoice.ActivityChoice2;
import playground.johannes.socialnetworks.sim.locationChoice.ActivityMover;
import playground.johannes.socialnetworks.sim.locationChoice.NegatedGibbsPlanSelector;
import playground.johannes.socialnetworks.survey.ivt2009.graph.io.SocialSparseGraphMLReader;

/**
 * @author illenberger
 *
 */
public class Simulator {
	
	private static final Logger logger = Logger.getLogger(Simulator.class);

	private StrategyManager strategyManager;
	
	private PseudoSim pseudoSim;
	
	private Population population;
	
	private Network network;
	
	private TravelTime travelTime;
	
	private EventsManagerImpl eventManager;
	
	private EventsToScore scorer;
	
//	private Map<Person, ChoiceSet> choiceSets;
	
	private SocialGraph socialNetwork;
	
	private NegatedGibbsPlanSelector selector;
//	private final Random random;
	
	public static void main(String args[]) {
		Config config = new Config();
		config.addModule("planCalcScore", new PlanCalcScoreConfigGroup());
		MatsimConfigReader creader = new MatsimConfigReader(config);
		creader.readFile(args[0]);
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario);
		netReader.readFile(config.getParam("network", "inputNetworkFile"));
		MatsimPopulationReader popReader = new MatsimPopulationReader(scenario);
		popReader.readFile(config.getParam("plans", "inputPlansFile"));
		
		SocialSparseGraphMLReader reader = new SocialSparseGraphMLReader();
		SocialGraph graph = reader.readGraph(config.getParam("socialnets", "graphfile"), scenario.getPopulation());
		
		String output = config.getParam("controler", "outputDirectory");
		long rndSeed = Long.parseLong(config.getParam("global", "randomSeed"));
		int iterations = Integer.parseInt(config.getParam("controler", "lastIteration"));
		Simulator sim = new Simulator(config, scenario.getNetwork(), scenario.getPopulation(), new Random(rndSeed), graph);
		sim.run(iterations, output);
	}
	
	public Simulator(Config config, Network network, Population population, Random random, SocialGraph graph) {
		this.network = network;
		this.population = population;
		this.socialNetwork = graph;
//		this.random = random;
		
		pseudoSim = new PseudoSim();
		eventManager = (EventsManagerImpl) EventsUtils.createEventsManager();
		
		scorer = new EventsToScore(population, new CharyparNagelScoringFunctionFactory((PlanCalcScoreConfigGroup) config.getModule("planCalcScore")));
		eventManager.addHandler(scorer);
		
		
		
		strategyManager = new StrategyManager();
		selector = new NegatedGibbsPlanSelector(10000.0, random);
		strategyManager.setPlanSelectorForRemoval(selector);

		strategyManager.setMaxPlansPerAgent(1);
//		PlanStrategy keepSelected = new PlanStrategyImpl(new BestPlanSelector());
		
		travelTime = new FreespeedTravelTimeCost(-6/3600.0, 0, 0);
		LeastCostPathCalculator router = new Dijkstra(network, (TravelCost) travelTime, travelTime);

		PlanStrategy changeLocation = new PlanStrategyImpl(new KeepSelected());
		
		ActivityMover mover = new ActivityMover(population.getFactory(), router, network);
		ActivityChoice2 choice = new ActivityChoice2(graph, mover, random);
		
		changeLocation.addStrategyModule(choice);
		
//		strategyManager.addStrategy(keepSelected, 1.0);
		strategyManager.addStrategy(changeLocation, 1.0);
	}
	
	public void run(long iterations, String output) {
		PopulationWriter writer = new PopulationWriter(population, network);
		writer.write(String.format("%1$s/%2$s.plans.xml", output, "initial"));
		
		scorer.reset(0);
		pseudoSim.run(population, network, travelTime, eventManager);
		scorer.finish();
		
		for(int i = 1; i < iterations; i++) {
			logger.info("Analyzing...");
			if(i > 0 && i % 1 == 0) {
				writer.write(String.format("%1$s/%2$s.plans.xml", output, i));
				String outDir = String.format("%1$s/analysis/%2$s/", output, i);
				new File(outDir).mkdirs();
				TravelDistanceTask task = new TravelDistanceTask(network, outDir);
				NearestLocation locTask = new NearestLocation(socialNetwork, outDir);
				PlanAnalyzerTaskComposite composite = new PlanAnalyzerTaskComposite();
				composite.addComponent(task);
				composite.addComponent(locTask);
				Map<String, Double> map = PlansAnalyzer.analyzeUnselectedPlans(population, composite);
//				Map<String, Double> map = PlansAnalyzer.analyzeSelectedPlans(population, composite);
				try {
					PlansAnalyzer.write(map, outDir + "summary.txt");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			logger.info(String.format("Simulating iteration %1$s...", i));
			step();
			
		}
	}
	
	public void step() {
		selector.notifyIterationStarts(null);
		/*
		 * get new state
		 */
		strategyManager.run(population);
		
		scorer.reset(0);
		pseudoSim.run(population, network, travelTime, eventManager);
		scorer.finish();
		
		double score_selected = 0;
		int cnt_selected = 0;
		double score = 0;
		int cnt = 0;
		for(Person person : population.getPersons().values()) {
			for(Plan plan : person.getPlans()) {
				if(plan.getScore() != null) {
				if(plan.isSelected()) {
					score_selected += plan.getScore();
					cnt_selected++;
				} else {
					score += plan.getScore();
					cnt++;
				}
				}
			}
		}
		selector.notifyIterationEnds(null);
		
		logger.info(String.format("Average selected plan score = %1$s.", score_selected/(double)cnt_selected));
		logger.info(String.format("Average unselected plan score = %1$s.", score/(double)cnt));
	}
}
