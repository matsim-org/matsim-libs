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
import org.matsim.api.basic.v01.BasicScenario;
import org.matsim.api.basic.v01.BasicScenarioImpl;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.basic.v01.population.BasicActivity;
import org.matsim.api.basic.v01.population.BasicLeg;
import org.matsim.api.basic.v01.population.BasicPerson;
import org.matsim.api.basic.v01.population.BasicPlan;
import org.matsim.api.basic.v01.population.BasicPopulation;
import org.matsim.api.basic.v01.population.BasicPopulationBuilder;
import org.matsim.core.api.experimental.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.NodeNetworkRoute;

import playground.dgrether.DgPaths;


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

	private NetworkLayer uselessNetwork;
	
	public BkIncomeTestScenarioCreator(NetworkLayer uselessNetwork) {
		this.uselessNetwork = uselessNetwork;
	}


	public BasicPopulation<BasicPerson<BasicPlan>> createPlans() {
		double firstHomeEndTime = 6.0 * 3600.0;
		double homeEndTime = firstHomeEndTime;
		log.info("starting plans creation...");
		
		BasicScenario scenario = new BasicScenarioImpl();
		
		BasicPopulation<BasicPerson<BasicPlan>> pop = scenario.getPopulation();
		BasicPopulationBuilder builder = pop.getBuilder();
		
		for (int i = 1; i <= 2000; i++) {
			BasicPerson p = builder.createPerson(scenario.createId(Integer.toString(i)));	

			//adding carPlan to person
			BasicPlan plan = builder.createPlan(p);
			plan.setSelected(true);
			p.getPlans().add(plan);
			
			BasicActivity act1 = builder.createActivityFromLinkId("h", id1);			
			act1.setEndTime(homeEndTime);
			plan.addActivity(act1);
			
			BasicLeg leg1Car = builder.createLeg(TransportMode.car);
			NodeNetworkRoute routeCar = new NodeNetworkRoute(this.uselessNetwork.getLink(id1), this.uselessNetwork.getLink(id4));
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
			
			BasicActivity act2 = builder.createActivityFromLinkId("w", id4);
			act2.setStartTime(7.0 * 3600.0);
			act2.setEndTime(15.0 * 3600.0);
//			act2.setDuration(8.0 * 3600.0);
			plan.addActivity(act2);
			
			BasicLeg leg2Car = builder.createLeg(TransportMode.car);
			routeCar = new NodeNetworkRoute(this.uselessNetwork.getLink(id4), this.uselessNetwork.getLink(id1));
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
			
			BasicActivity act3 = builder.createActivityFromLinkId("h", id1);
			plan.addActivity(act3);
			
			//adding ptPlan to person
			plan = builder.createPlan(p);
			p.getPlans().add(plan);
			//plan.setSelected(true);
			
			plan.addActivity(act1);
			
			BasicLeg leg1Pt = builder.createLeg(TransportMode.pt);
//			BasicRouteImpl routePt = new BasicRouteImpl(id1, id4);
//			List<Id> linkidsPt = new ArrayList<Id>();
//			routePt.setLinkIds(linkidsPt);
//			leg1Pt.setRoute(routePt);
			plan.addLeg(leg1Pt);

			plan.addActivity(act2);
			
			BasicLeg leg2Pt = builder.createLeg(TransportMode.pt);
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
		String outfile = DgPaths.SHAREDSVN + "studies/bkick/oneRouteTwoModeIncomeTest/plans.xml";
		String networkFile = DgPaths.SHAREDSVN + "studies/bkick/oneRouteTwoModeIncomeTest/network.xml";
		NetworkLayer uselessNetwork = new NetworkLayer();
		MatsimNetworkReader reader = new MatsimNetworkReader(uselessNetwork);
		reader.readFile(networkFile);
		Gbl.createWorld();
		Gbl.getWorld().setNetworkLayer(uselessNetwork);
		
		
		BkIncomeTestScenarioCreator pc = new BkIncomeTestScenarioCreator(uselessNetwork);
		BasicPopulation<BasicPerson<BasicPlan>> pop = pc.createPlans();
		PopulationWriter writer = new PopulationWriter(pop, outfile);
		writer.write();
		log.info("plans written");
		log.info("finished!");
	}

}
