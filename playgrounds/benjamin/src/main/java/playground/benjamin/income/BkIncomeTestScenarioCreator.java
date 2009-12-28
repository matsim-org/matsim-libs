/* *********************************************************************** *
 * project: org.matsim.*																															*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.benjamin.income;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.NodeNetworkRouteImpl;

import playground.dgrether.DgPaths;
import playground.dgrether.utils.ListUtils;


/**
 * @author dgrether
 *
 */
public class BkIncomeTestScenarioCreator {

	private static final Logger log = Logger.getLogger(BkIncomeTestScenarioCreator.class);
	
	private Id id1 = new IdImpl(1);
	private Id id2 = new IdImpl(2);
	private Id id3 = new IdImpl(3);
	private Id id4 = new IdImpl(4);
	private Id id5 = new IdImpl(5);
	private Id id6 = new IdImpl(6);
	private Id id7 = new IdImpl(7);

	private final NetworkImpl uselessNetwork;
	private final ScenarioImpl scenario;
	
	public BkIncomeTestScenarioCreator(ScenarioImpl scenario) {
		this.uselessNetwork = scenario.getNetwork();
		this.scenario = scenario;
	}


	public Population createPlans() {
		double firstHomeEndTime = 6.0 * 3600.0;
		double homeEndTime = firstHomeEndTime;
		log.info("starting plans creation...");
		
		Population pop = this.scenario.getPopulation();
		PopulationFactory builder = pop.getFactory();
		
		for (int i = 1; i <= 2000; i++) {
			Person p = builder.createPerson(scenario.createId(Integer.toString(i)));	

			//adding carPlan to person
			Plan plan = builder.createPlan();
			p.addPlan(plan);
			plan.setSelected(true);
			
			Activity act1 = builder.createActivityFromLinkId("h", id1);			
			act1.setEndTime(homeEndTime);
			plan.addActivity(act1);
			
			Leg leg1Car = builder.createLeg(TransportMode.car);
			NodeNetworkRouteImpl routeCar = new NodeNetworkRouteImpl(this.uselessNetwork.getLink(id1), this.uselessNetwork.getLink(id4));
			//this would be so nice
			List<Id> linkidsCar = new ArrayList<Id>();
			linkidsCar.add(id2);
			linkidsCar.add(id3);
//			routeCar.setLinkIds(linkidsCar);
			// but this is reality
			routeCar.setNodes(ListUtils.<Node>makeList(this.uselessNetwork.getNode(new IdImpl("2")), 
					this.uselessNetwork.getNode(new IdImpl("3")), 
					this.uselessNetwork.getNode(new IdImpl("4"))));
			leg1Car.setRoute(routeCar);
			plan.addLeg(leg1Car);
			
			Activity act2 = builder.createActivityFromLinkId("w", id4);
			act2.setStartTime(7.0 * 3600.0);
			act2.setEndTime(15.0 * 3600.0);
//			act2.setDuration(8.0 * 3600.0);
			plan.addActivity(act2);
			
			Leg leg2Car = builder.createLeg(TransportMode.car);
			routeCar = new NodeNetworkRouteImpl(this.uselessNetwork.getLink(id4), this.uselessNetwork.getLink(id1));
			//in a beautiful world we would do...
			linkidsCar = new ArrayList<Id>();
			linkidsCar.add(id5);
			linkidsCar.add(id6);
			linkidsCar.add(id7);
//			routeCar.setLinkIds(linkidsCar);
			//but not with mankind
			
			
			routeCar.setNodes(ListUtils.<Node>makeList(this.uselessNetwork.getNode(new IdImpl("5")),
					this.uselessNetwork.getNode(new IdImpl("6")),
					this.uselessNetwork.getNode(new IdImpl("7")),
					this.uselessNetwork.getNode(new IdImpl("8")),
					this.uselessNetwork.getNode(new IdImpl("1"))));
			leg2Car.setRoute(routeCar);
			plan.addLeg(leg2Car);
			
			Activity act3 = builder.createActivityFromLinkId("h", id1);
			plan.addActivity(act3);
			
			//adding ptPlan to person
			plan = builder.createPlan();
			p.addPlan(plan);
			//plan.setSelected(true);
			
			plan.addActivity(act1);
			
			Leg leg1Pt = builder.createLeg(TransportMode.pt);
//			BasicRouteImpl routePt = new BasicRouteImpl(id1, id4);
//			List<Id> linkidsPt = new ArrayList<Id>();
//			routePt.setLinkIds(linkidsPt);
//			leg1Pt.setRoute(routePt);
			plan.addLeg(leg1Pt);

			plan.addActivity(act2);
			
			Leg leg2Pt = builder.createLeg(TransportMode.pt);
//			routePt = new BasicRouteImpl(id4, id1);
//			linkidsPt = new ArrayList<Id>();
//			routePt.setLinkIds(linkidsPt);
//			leg2Pt.setRoute(routePt);
			plan.addLeg(leg2Pt);
			
			plan.addActivity(act3);
			
			pop.addPerson(p);
//			homeEndTime++;				
		}
		log.info("created population...");
		return pop;
	}
	
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		ScenarioImpl scenario = new ScenarioImpl();
		String outfile = DgPaths.SHAREDSVN + "studies/bkick/oneRouteTwoModeIncomeTest/plans.xml";
		String networkFile = DgPaths.SHAREDSVN + "studies/bkick/oneRouteTwoModeIncomeTest/network.xml";
		NetworkImpl uselessNetwork = scenario.getNetwork();
		new MatsimNetworkReader(uselessNetwork).readFile(networkFile);
		
		BkIncomeTestScenarioCreator pc = new BkIncomeTestScenarioCreator(scenario);
		Population pop = pc.createPlans();
		new PopulationWriter(pop).writeFile(outfile);
		log.info("plans written");
		log.info("finished!");
	}

}
