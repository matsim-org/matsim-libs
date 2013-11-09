/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,     *
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

package tutorial.programming.multipleSubpopulations;

import java.util.Iterator;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.PlanStrategyRegistrar;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.TimeAllocationMutator;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

/**
 * This example illustrates how to go about modelling subpopulations, 
 * and specifically dealing with the replanning strategies for the different
 * subpopulations. Two subpopulations are created:
 * <ol>
 * 		<li> <b>time</b> - agents who will use the {@link TimeAllocationMutator}
 * 			 strategy to replan and adapt their departure times; and
 * 		<li> <b>reroute</b> - agents who will use the {@link ReRoute}
 * 			 strategy to replan and adapt their routes.
 *  </ol>
 *  For both these subpopulations the indicated strategy will be chosen 20% of
 *  the time while the balance will use the <em>ChangeExpBeta</em> strategy.
 *  
 *  The results are best visualised using senozon's Via, and colouring the two
 *  subpopulations differently. To do that you will need two lists, each 
 *  containing the {@link Id}s of all the agents in that specific subpopulation.
 *  In the input folder, two files are added for you with the {@link Id}s:
 *  <code>time.txt</code> for those agents adapting their activity timing, and
 *  <code>route.txt</code> for those agents adapting their routes. 
 *  @author nagel, jwjoubert
 */
public class SubpopulationsExample {
	final static String EQUIL_NETWORK = "./examples/equil/network.xml";
	final static String PLANS = "./examples/tutorial/programming/MultipleSubpopulations/plans.xml";
	final static String OBJECT_ATTRIBUTES = "./examples/tutorial/programming/MultipleSubpopulations/personAtrributes.xml";
	final static String CONFIG = "./examples/tutorial/programming/MultipleSubpopulations/config.xml";
	final static String OUTPUT = "./output/";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		/* Set up network and plans. */
		MatsimNetworkReader mnr = new MatsimNetworkReader(sc);
		mnr.parse(EQUIL_NETWORK);
		createPopulation(sc, "time", 100);
		createPopulation(sc, "reroute", 100);
		new PopulationWriter(sc.getPopulation(), sc.getNetwork()).write(PLANS);
		new ObjectAttributesXmlWriter(sc.getPopulation().getPersonAttributes()).writeFile(OBJECT_ATTRIBUTES);
		
		Config config = ConfigUtils.createConfig(); 
		ConfigUtils.loadConfig(config, CONFIG);
		config.plans().setInputFile(PLANS);
		config.plans().setInputPersonAttributeFile(OBJECT_ATTRIBUTES);
		config.plans().setSubpopulationAttributeName("subpopulation"); /* This is the default anyway. */
		config.network().setInputFile(EQUIL_NETWORK);
		config.controler().setOutputDirectory(OUTPUT);
		
		/* Strategies set up in the 'strategy' module of the config file are
		 * only applicable to the 'default' population, i.e. those agents without 
		 * a subpopulation attribute. For all agents with a subpopulation
		 * attribute, you need to set up the suite of strategies for the 
		 * subpopulation(s).
		 * 
		 * In the example below, we assume that the config file may already have
		 * strategies set up. Since we cannot overwrite an existing strategy, we
		 * need to find the next available number. */
		long maxStrategyId = 1;
		Iterator<StrategySettings> iterator = config.strategy().getStrategySettings().iterator();
		while(iterator.hasNext()){
			maxStrategyId = Math.max(maxStrategyId, Long.parseLong(iterator.next().getId().toString()));
		}
		long currentStrategyId = maxStrategyId + 1;
				
		/* Set up the subpopulations. */
		{
			/* Set up the `reroute' subpopulation to consider rerouting as a 
			 * strategy, 20% of the time, and the balance using ChangeExpBeta. */
			StrategySettings rerouteStrategySettings = new StrategySettings(new IdImpl(currentStrategyId++));
			rerouteStrategySettings.setModuleName(PlanStrategyRegistrar.Names.ReRoute.toString());
			rerouteStrategySettings.setSubpopulation("reroute");
			rerouteStrategySettings.setProbability(0.2);
			config.strategy().addStrategySettings(rerouteStrategySettings);

			StrategySettings changeExpBetaStrategySettings = new StrategySettings(new IdImpl(currentStrategyId++));
			changeExpBetaStrategySettings.setModuleName(PlanStrategyRegistrar.Selector.ChangeExpBeta.toString());
			changeExpBetaStrategySettings.setSubpopulation("reroute");
			changeExpBetaStrategySettings.setProbability(0.8);
			config.strategy().addStrategySettings(changeExpBetaStrategySettings);
		}
		{
			/* Set up the 'time' subpopulation to only consider time allocation 
			 * as a strategy, 20% of the time, and the balance using ChangeExpBeta. */
			StrategySettings timeStrategySettings = new StrategySettings(new IdImpl(currentStrategyId++));
			timeStrategySettings.setModuleName(PlanStrategyRegistrar.Names.TimeAllocationMutator.toString());
			timeStrategySettings.setSubpopulation("time");
			timeStrategySettings.setProbability(0.2);
			config.strategy().addStrategySettings(timeStrategySettings);
			
			StrategySettings changeExpBetaStrategySettings = new StrategySettings(new IdImpl(currentStrategyId++));
			changeExpBetaStrategySettings.setModuleName(PlanStrategyRegistrar.Selector.ChangeExpBeta.toString());
			changeExpBetaStrategySettings.setSubpopulation("time");
			changeExpBetaStrategySettings.setProbability(0.8);
			config.strategy().addStrategySettings(changeExpBetaStrategySettings);
		}
		
		/* Run the model. */
		Controler controler = new Controler(config);
		controler.setOverwriteFiles(true);
		controler.run();
	}
	
	/**
	 * Private class to create individuals with a specific prefix in their 
	 * {@link Person} {@link Id}s to distinguish the subpopulations.
	 * @param scenario
	 * @param prefix to distinguish the subpopulation.
	 * @param number of {@link Person}s to create.
	 */
	private static void createPopulation(Scenario scenario, String prefix, int number){
		PopulationFactory pf = scenario.getPopulation().getFactory();
		
		for(int i = 0; i < number; i++){
			Person person = pf.createPerson(new IdImpl(prefix + "_" + i));
			
			/* Create basic home-work-home activities on equil network. */
			Plan plan = pf.createPlan();
			Activity h1 = pf.createActivityFromCoord("h", scenario.getNetwork().getNodes().get(new IdImpl("1")).getCoord());
			h1.setEndTime(6*3600);
			Activity w = pf.createActivityFromCoord("w", scenario.getNetwork().getNodes().get(new IdImpl("13")).getCoord());
			w.setEndTime(17*3600);
			Activity h2 = pf.createActivityFromCoord("h", scenario.getNetwork().getNodes().get(new IdImpl("1")).getCoord());
			h2.setStartTime(18*3600);
			
			/* Add the activities to the plan. */
			plan.addActivity(h1);
			plan.addLeg(pf.createLeg("car"));
			plan.addActivity(w);
			plan.addLeg(pf.createLeg("car"));
			plan.addActivity(h2);
			
			/* Set the subpopulation attribute. */
			scenario.getPopulation().getPersonAttributes().putAttribute(person.getId().toString(), "subpopulation", prefix);
			
			person.addPlan(plan);
			scenario.getPopulation().addPerson(person);
		}
	}

}
