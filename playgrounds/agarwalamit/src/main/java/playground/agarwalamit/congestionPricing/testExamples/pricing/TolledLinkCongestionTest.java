/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.congestionPricing.testExamples.pricing;

import java.io.File;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.replanning.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.vsp.congestion.controler.MarginalCongestionPricingContolerListener;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;
import playground.vsp.congestion.handlers.TollHandler;
import playground.vsp.congestion.routing.TollDisutilityCalculatorFactory;

/**
 * @author amit
 */

public class TolledLinkCongestionTest {

	private Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());;
	private Link lo1;
	private Link ld1;
	private Link lo2;
	private Link ld2;
	
	private final String outputDir = "./output/"+this.getClass().getSimpleName()+"/";
	
	public static void main(String[] args) {
		
		new TolledLinkCongestionTest().run();
	}
	
	public void run() {
		
		if(!new File(outputDir+"/input/").exists()) {
			new File(outputDir+"/input/").mkdirs();
		}
		
		createTolledNetwork();
		createDemand();
		createConfig();
		Controler controler = new Controler(sc);
		
		TollHandler tollHandler = new TollHandler(sc);
		final TollDisutilityCalculatorFactory fact = new TollDisutilityCalculatorFactory(tollHandler);
		
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindTravelDisutilityFactory().toInstance(fact);
			}
		});
		controler.addControlerListener(new MarginalCongestionPricingContolerListener(sc, tollHandler, new CongestionHandlerImplV3(controler.getEvents(), (ScenarioImpl) sc)));
		controler.run();
	}
	
	private void createConfig(){
		
		Config config = sc.getConfig();
		
		config.controler().setOutputDirectory(outputDir+"/output/");
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(20);
		config.controler().setWriteEventsInterval(10);
		config.controler().setMobsim("qsim");
		config.controler().setOverwriteFileSetting(true ? OverwriteFileSetting.deleteDirectoryIfExists:OverwriteFileSetting.failIfDirectoryExists);
		
		StrategySettings reRoute = new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
		reRoute.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute.name());
		reRoute.setWeight(0.10);
		config.strategy().addStrategySettings(reRoute);
		config.strategy().setFractionOfIterationsToDisableInnovation(0.7);
		
		ActivityParams o1 = new ActivityParams("o1");
		ActivityParams o2 = new ActivityParams("o2");
		ActivityParams d1 = new ActivityParams("d1");
		ActivityParams d2 = new ActivityParams("d2");
		
		o1.setTypicalDuration(8*3600);
		o2.setTypicalDuration(8*3600);
		d1.setTypicalDuration(8*3600);
		d2.setTypicalDuration(8*3600);
		
		config.planCalcScore().addActivityParams(o1);
		config.planCalcScore().addActivityParams(o2);
		config.planCalcScore().addActivityParams(d1);
		config.planCalcScore().addActivityParams(d2);
	}

	private void createDemand(){

		Population pop = sc.getPopulation();
		PopulationFactory fact = pop.getFactory();

		for (int i = 1; i <= 600; i++){

			Person p =	fact.createPerson(Id.createPersonId(i));
			Plan plan = fact.createPlan();
			p.addPlan(plan);
			Leg leg = fact.createLeg(TransportMode.car);
			Activity home;
			Activity work;
			
			if(i%3==0){// o1 --d1
				home = fact.createActivityFromCoord("o1", lo1.getCoord());
				home.setEndTime(7*3600+i);
				work = fact.createActivityFromCoord("d1", ld1.getCoord());
			} else if(i%2==0){// o1 --d2
				home = fact.createActivityFromCoord("o1", lo1.getCoord());
				home.setEndTime(7*3600+i);
				work = fact.createActivityFromCoord("d2", ld2.getCoord());
			} else {// o2 --d2
				home = fact.createActivityFromCoord("o2", lo2.getCoord());
				home.setEndTime(7*3600+i);
				work = fact.createActivityFromCoord("d2", ld2.getCoord());
			}
			plan.addActivity(home);
			plan.addLeg(leg);
			plan.addActivity(work);
			pop.addPerson(p);
		}
		new PopulationWriter(pop).write(outputDir+"/input/initialPlans.xml.gz");
	}


	/*
	 * 								o
	 * 								|
	 * 								|
	 * 								o2
	 * 								|
	 * 								|
	 * 								o
	 * 								|
	 * 								|
	 * 								5
	 * 								|
	 * 								|
	 * o--o1--o----1----o----2------o-----3----o---d1---o
	 * 							 \  |  /
	 * 							  |	| |
	 * 							  |	6 |
	 * 							  |	| |
	 * 							  |	| |
	 * 						     /	o  \
	 * 								|
	 * 								|
	 * 								d2
	 * 								|
	 * 								|
	 * 								o
	 * 
	 */
	private void createTolledNetwork (){

		NetworkImpl network = (NetworkImpl) sc.getNetwork();

		// nodes between o1-d1 (all horizonal links)
		Node no1 = network.createAndAddNode(Id.createNodeId("o1"), sc.createCoord(-100, 0));
		Node n1 = network.createAndAddNode(Id.createNodeId(1), sc.createCoord(0, 0));
		Node n2 = network.createAndAddNode(Id.createNodeId(2), sc.createCoord(1000, 0));
		Node n3 = network.createAndAddNode(Id.createNodeId(3), sc.createCoord(2000, 0));
		Node n4 = network.createAndAddNode(Id.createNodeId(4), sc.createCoord(3000, 0));
		Node nd1 = network.createAndAddNode(Id.createNodeId("d1"), sc.createCoord(3100, 0));

		lo1 = network.createAndAddLink(Id.createLinkId("o1"), no1, n1, 100, 20, 2700, 1);
		Link l1 = network.createAndAddLink(Id.createLinkId(1), n1, n2, 1000, 20, 2700, 1);
		Link l2 = network.createAndAddLink(Id.createLinkId(2), n2, n3, 1000, 20, 2700, 1);
		Link l3 = network.createAndAddLink(Id.createLinkId(3), n3, n4, 1000, 20, 2700, 1);
		ld1 = network.createAndAddLink(Id.createLinkId("d1"), n4, nd1, 100, 20, 2700, 1);

		// nodes between o2-d2 (all vertical links)
		Node no2 = network.createAndAddNode(Id.createNodeId("o2"), sc.createCoord(2000, 1100));
		Node n5 = network.createAndAddNode(Id.createNodeId(5), sc.createCoord(2000, 1000));
		Node n6 = network.createAndAddNode(Id.createNodeId(6), sc.createCoord(2000,-1000));
		Node nd2 = network.createAndAddNode(Id.createNodeId("d2"), sc.createCoord(2000, -1100));

		lo2 = network.createAndAddLink(Id.createLinkId("o2"), no2, n5, 100, 20, 2700, 1);
		Link l5 = network.createAndAddLink(Id.createLinkId(5), n5, n3, 3000, 30, 2700, 1);
		//bottleneck link
		Link l6 = network.createAndAddLink(Id.createLinkId(6), n3, n6, 800, 20, 1350, 1);
		ld2 = network.createAndAddLink(Id.createLinkId("d2"), n6, nd2, 100, 20, 2700, 1);

		// an alternative link with higher disutility
		Link l7 = network.createAndAddLink(Id.createLinkId(7), n1, n6, 12500, 20, 2700, 1);
		
		new NetworkWriter(network).write(outputDir+"/input/tolledNetwork.xml");
	}
}
