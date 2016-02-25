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

import org.matsim.api.core.v01.Coord;
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
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

import playground.vsp.congestion.controler.MarginalCongestionPricingContolerListener;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;
import playground.vsp.congestion.handlers.CongestionHandlerImplV4;
import playground.vsp.congestion.handlers.TollHandler;
import playground.vsp.congestion.routing.TollDisutilityCalculatorFactory;

/**
 * @author amit
 */

class CongestionPricingTestExample {

	CongestionPricingTestExample (String congestionImpl){
		this.isComparing = true;
		this.congestionImpl = congestionImpl;
	}

	CongestionPricingTestExample () {
		this.isComparing = false;
	}

	private Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());;
	private Link lo;
	private Link ld1;
	private Link ld2;
	private String congestionImpl = "noToll";
	private boolean isComparing;

	private final String outputDir = "../../../repos/shared-svn/papers/2014/congestionInternalization/implV4/hEART/testExample/";

	public static void main(String[] args) {

		// no toll case
		new CongestionPricingTestExample().run();

		// tolled cases
		String [] congestionImpls = {"implV3","implV4","implV6"};

		for (String str :congestionImpls) {
			new CongestionPricingTestExample(str).run();
		}

		new PricingTestAnalyser().run();
	}

	void run() {

		if(!new File(outputDir+"/input/").exists()) {
			new File(outputDir+"/input/").mkdirs();
		}

		createTolledNetwork();
		createDemand();
		createConfig();
		Controler controler = new Controler(sc);

		if(isComparing){
			TollHandler tollHandler = new TollHandler(sc);
			final TollDisutilityCalculatorFactory fact = new TollDisutilityCalculatorFactory(tollHandler, controler.getConfig().planCalcScore());

			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bindCarTravelDisutilityFactory().toInstance(fact);
				}
			});

			switch (congestionImpl) {
			case "implV3":
				controler.addControlerListener(new MarginalCongestionPricingContolerListener(sc, tollHandler, new CongestionHandlerImplV3(controler.getEvents(),  (MutableScenario)sc)));	
				break;
			case "implV4":
				controler.addControlerListener(new MarginalCongestionPricingContolerListener(sc, tollHandler, new CongestionHandlerImplV4(controler.getEvents(),  sc)));
				break;
			case "implV6":
//				services.addControlerListener(new MarginalCongestionPricingContolerListener(sc, tollHandler, new CongestionHandlerImplV6(services.getEvents(),  sc)));
				break;
			default:
				break;
			}
		}
		controler.run();
	}

	private void createConfig(){

		Config config = sc.getConfig();

		config.controler().setOutputDirectory(outputDir+"/output/"+congestionImpl);
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(20);
		config.controler().setWriteEventsInterval(10);
		config.controler().setMobsim("qsim");
		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );

		config.qsim().setEndTime(9*3600.);

		StrategySettings reRoute = new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
		reRoute.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute.name());
		reRoute.setWeight(0.10);
		config.strategy().addStrategySettings(reRoute);
		config.strategy().setFractionOfIterationsToDisableInnovation(0.7);

		StrategySettings changeExpBeta = new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
		changeExpBeta.setStrategyName("ChangeExpBeta");
		changeExpBeta.setWeight(0.9);
		config.strategy().addStrategySettings(changeExpBeta);

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

		new ConfigWriter(config).write(outputDir+"/input/input_config.xml.gz");
	}

	private void createDemand(){

		Population pop = sc.getPopulation();
		PopulationFactory fact = pop.getFactory();

		for (int i = 1; i <= 400; i++){

			Person p =	fact.createPerson(Id.createPersonId(i));
			Plan plan = fact.createPlan();
			p.addPlan(plan);
			Leg leg = fact.createLeg(TransportMode.car);
			Activity home;
			Activity work;

			if(i%2==0){// o --d1
				home = fact.createActivityFromCoord("o1", lo.getCoord());
				home.setEndTime(7*3600+i);
				work = fact.createActivityFromCoord("d1", ld1.getCoord());
			} else /*if(i%2==0)*/ { // o --d2
				home = fact.createActivityFromCoord("o1", lo.getCoord());
				home.setEndTime(7*3600+i);
				work = fact.createActivityFromCoord("d2", ld2.getCoord());
			} 
			plan.addActivity(home);
			plan.addLeg(leg);
			plan.addActivity(work);
			pop.addPerson(p);
		}
		new PopulationWriter(pop).write(outputDir+"/input/input_plans.xml.gz");
	}


	/*
	 * o--o--o----1----o----2------o-----3----o---d1---o
	 * 		  \					 \  |  /
	 * 			\				  |	| |
	 * 			 \				  |	4 |
	 * 			  5				  |	| |
	 * 			   \			 /	|  \
	 * 				 \______________o  
	 * 								|
	 * 								d2
	 * 								|
	 * 								o
	 * 
	 */
	private void createTolledNetwork (){

		NetworkImpl network = (NetworkImpl) sc.getNetwork();

		// nodes between o-d1 (all horizonal links)
		double x = -100;
		Node no = network.createAndAddNode(Id.createNodeId("o"), new Coord(x, (double) 0));
		Node n1 = network.createAndAddNode(Id.createNodeId(1), new Coord((double) 0, (double) 0));
		Node n2 = network.createAndAddNode(Id.createNodeId(2), new Coord((double) 1000, (double) 0));
		Node n3 = network.createAndAddNode(Id.createNodeId(3), new Coord((double) 2000, (double) 0));
		Node n4 = network.createAndAddNode(Id.createNodeId(4), new Coord((double) 3000, (double) 0));
		Node nd1 = network.createAndAddNode(Id.createNodeId("d1"), new Coord((double) 3100, (double) 0));

		lo = network.createAndAddLink(Id.createLinkId("o"), no, n1, 100, 15, 3600, 1);
		network.createAndAddLink(Id.createLinkId(1), n1, n2, 1000, 15, 3600, 1);
		network.createAndAddLink(Id.createLinkId(2), n2, n3, 1000, 15, 3600, 1);
		network.createAndAddLink(Id.createLinkId(3), n3, n4, 1000, 15, 3600, 1);
		ld1 = network.createAndAddLink(Id.createLinkId("d1"), n4, nd1, 100, 15, 3600, 1);

		double y1 = -1000;
		Node n6 = network.createAndAddNode(Id.createNodeId(6), new Coord((double) 2000, y1));
		double y = -1100;
		Node nd2 = network.createAndAddNode(Id.createNodeId("d2"), new Coord((double) 2000, y));

		//bottleneck link
		network.createAndAddLink(Id.createLinkId(4), n3, n6, 600, 15, 600, 1);
		ld2 = network.createAndAddLink(Id.createLinkId("d2"), n6, nd2, 100, 15, 1500, 1);

		// an alternative link with higher disutility
		network.createAndAddLink(Id.createLinkId(5), n1, n6, 12750, 15, 2700, 1);

		new NetworkWriter(network).write(outputDir+"/input/input_network.xml");
	}
}
