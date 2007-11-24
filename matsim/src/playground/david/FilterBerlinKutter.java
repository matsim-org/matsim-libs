/* *********************************************************************** *
 * project: org.matsim.*
 * FilterBerlinKutter.java
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

package playground.david;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.matsim.basic.v01.Id;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueLink;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.Node;
import org.matsim.plans.Leg;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansWriter;
import org.matsim.plans.algorithms.PersonAlgorithm;
import org.matsim.world.World;

class FilterPersons extends PersonAlgorithm{

	public static Set<Node> relevantFromNodes = new HashSet<Node>();
	public static Set<Node> relevantToNodes = new HashSet<Node>();
	
	public FilterPersons() {
		super();
		//Find relevant nodes to look for
		QueueLink link1 = FilterBerlinKutter.network.getLinks().get(new Id(1655));
		relevantFromNodes.add(link1.getFromNode());
		relevantToNodes.add(link1.getToNode());
		link1 = FilterBerlinKutter.network.getLinks().get(new Id(1659));
		relevantFromNodes.add(link1.getFromNode());
		relevantToNodes.add(link1.getToNode());
		link1 = FilterBerlinKutter.network.getLinks().get(new Id(1663));
		relevantFromNodes.add(link1.getFromNode());
		relevantToNodes.add(link1.getToNode());
		link1 = FilterBerlinKutter.network.getLinks().get(new Id(1668));
		relevantFromNodes.add(link1.getFromNode());
		relevantToNodes.add(link1.getToNode());
	}

	@Override
	public void run(Person person) {
		// check for selected plans routes, if any of the relevant nodes shows up
		Plan plan = person.getSelectedPlan();
		for (int jj = 0; jj < plan.getActsLegs().size(); jj++) {
			if (jj % 2 == 0) {
			}else {
				Leg leg = (Leg)plan.getActsLegs().get(jj);
				// route
				if (leg.getRoute() != null) {
					ArrayList<Node> nodes = leg.getRoute().getRoute();
					int count = 0;
					for (Node node : nodes) {
						if (relevantFromNodes.contains(node)) count++;
						else if (relevantToNodes.contains(node)) count++;
					}
					if( count == 2 ) {
						try {
							FilterBerlinKutter.relevantPopulation.addPerson(person);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}
	
}

public class FilterBerlinKutter {
	public static Plans relevantPopulation;
	public static QueueNetworkLayer network;


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//String popFileName = "..\\..\\tmp\\studies\\berlin-wip\\kutter_population\\DSkutter010car_bln.router_wip.plans.v4.xml";
		String netFileName = "..\\..\\tmp\\studies\\berlin-wip\\network\\wip_net.xml";

		String popFileName = "..\\..\\tmp\\studies\\berlin-wip\\kutter_population\\30.plans.xml";
		String outpopFileName = "..\\..\\tmp\\studies\\berlin-wip\\kutter_population\\30_Jakob-Kaiser-RingONLY.plans.v4.xml";

		Gbl.startMeasurement();
		Gbl.createConfig(args);
		
		World world = Gbl.getWorld();
		
		network = new QueueNetworkLayer();
		new MatsimNetworkReader(network).readFile(netFileName);
		world.setNetworkLayer(network);
		
		relevantPopulation = new Plans(false);
		Plans population = new MyPopulation();
		MatsimPlansReader plansReader = new MatsimPlansReader(population);
		population.addAlgorithm(new FilterPersons());
		plansReader.readFile(popFileName);
//		population.runAlgorithms();

		PlansWriter plansWriter = new PlansWriter(relevantPopulation, outpopFileName, "v4");
		plansWriter.write();
	}

}
