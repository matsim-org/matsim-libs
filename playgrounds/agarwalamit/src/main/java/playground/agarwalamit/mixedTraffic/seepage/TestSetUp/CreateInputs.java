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
package playground.agarwalamit.mixedTraffic.seepage.TestSetUp;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.misc.Time;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author amit
 */
public class CreateInputs {

	private Scenario scenario = SeepageControler.sc ;
	private Link link1;
	private Link link2;
	private Link link3;
	private String outputDir = SeepageControler.outputDir;
	private List<String> mainModes = SeepageControler.mainModes;
	
	private void createConfig(){
		Config config = scenario.getConfig();
		config.qsim().setFlowCapFactor(1.0);
		config.qsim().setStorageCapFactor(1.0);
		config.qsim().setMainModes(mainModes);

		config.setParam("seepage", "isSeepageAllowed", "true");
		config.setParam("seepage", "seepMode", SeepageControler.seepMode);
		config.setParam("seepage","isSeepModeStorageFree", SeepageControler.isSeepModeStorageFree);
		
		config.qsim().setStuckTime(50*3600);
		config.qsim().setEndTime(18*3600);
//		config.controler().setOutputDirectory(outputDir);
//		config.controler().setLastIteration(0);
		config.plansCalcRoute().setNetworkModes(mainModes);
//		config.plansCalcRoute().setTeleportedModeSpeed("bike", 4.17);
		config.plans().setInputFile(outputDir+"/plans.xml");
		config.network().setInputFile(outputDir+"/network.xml");
		
		
		ActivityParams workAct = new ActivityParams("w");
		workAct.setTypicalDuration(8*3600);
		config.planCalcScore().addActivityParams(workAct);

		ActivityParams homeAct = new ActivityParams("h");
		homeAct.setTypicalDuration(12*3600);
		config.planCalcScore().addActivityParams(homeAct);
		
//		StrategySettings expChangeBeta = new StrategySettings(Id.create("1",StrategySettings.class));
//		expChangeBeta.setModuleName("ChangeExpBeta");
//		expChangeBeta.setProbability(1.0);
//		config.strategy().addStrategySettings(expChangeBeta);
		
		new ConfigWriter(scenario.getConfig()).write(outputDir+"/config.xml");
	}

	private void createNetwork(){
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		network.setCapacityPeriod(Time.parseTime("1:00:00"));
		Node node1 = network.createAndAddNode(Id.createNodeId("1"), scenario.createCoord(-100.0,0.0));
		Node node2 = network.createAndAddNode(Id.createNodeId("2"), scenario.createCoord( 0.0,  0.0));
		Node node3 = network.createAndAddNode(Id.createNodeId("3"), scenario.createCoord( 0.0,1000.0));
		Node node4 = network.createAndAddNode(Id.createNodeId("4"), scenario.createCoord( 100.0,1000.0));

		Set<String> allowedModes = new HashSet<String>(); allowedModes.addAll(Arrays.asList("car","walk"));

		 link1 = network.createAndAddLink(Id.createLinkId("-1"), node1, node2, 1000, 25, 6000, 1, null, "22"); 
		 link2 = network.createAndAddLink(Id.createLinkId("1"), node2, node3, 1000, 25, 100, 1, null, "22");	
		 link3 = network.createAndAddLink(Id.createLinkId("2"), node3, node4, 1000, 25, 6000, 1, null, "22");
		 link1.setAllowedModes(allowedModes);link2.setAllowedModes(allowedModes);link3.setAllowedModes(allowedModes);
		new NetworkWriter(network).write(outputDir+"/network.xml");
	}
	
	private void createPlans(){
		// 20000 walk persons are on the link 1, 0710/20/25/30 cars are present on the link2
		// destination of all agents is link 3
		Population population = scenario.getPopulation();
		for(int i=1;i<=100;i++){
			Id<Person> id = Id.createPersonId(i);
			Person p = population.getFactory().createPerson(id);
			population.addPerson(p);
			Plan plan = population.getFactory().createPlan();
			p.addPlan(plan);
			
			Activity a1 = population.getFactory().createActivityFromLinkId("h", link1.getId());
			
			Leg leg;
			if(i%2==0){
				leg = population.getFactory().createLeg(TransportMode.car);
				a1.setEndTime(0*3600+i*8);
			} else {
				a1.setEndTime(0*3600+i*2);
				leg = population.getFactory().createLeg(TransportMode.bike);
			}
			LinkNetworkRouteFactory factory = new LinkNetworkRouteFactory();
			NetworkRoute route = (NetworkRoute) factory.createRoute(link1.getId(), link3.getId());
			route.setLinkIds(link1.getId(), Arrays.asList(link2.getId()), link3.getId());
			leg.setRoute(route);
			
			plan.addActivity(a1);
			plan.addLeg(leg);
			
			Activity a2 = population.getFactory().createActivityFromLinkId("w", link3.getId());
			plan.addActivity(a2);
		}
		new PopulationWriter(population).write(outputDir+"/plans.xml");
	}

	public void run() {
		createNetwork();
		createPlans();
		createConfig();
	}
}
