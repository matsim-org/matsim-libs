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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Node;
import org.matsim.core.api.population.CarRoute;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.world.World;

class FilterPersons extends AbstractPersonAlgorithm{

	public static Set<Node> relevantFromNodes = new HashSet<Node>();
	public static Set<Node> relevantToNodes = new HashSet<Node>();

	public FilterPersons() {
		super();
		//Find relevant nodes to look for
		Link link1 = FilterBerlinKutter.network.getLinks().get(new IdImpl(1655));
		relevantFromNodes.add(link1.getFromNode());
		relevantToNodes.add(link1.getToNode());
		link1 = FilterBerlinKutter.network.getLinks().get(new IdImpl(1659));
		relevantFromNodes.add(link1.getFromNode());
		relevantToNodes.add(link1.getToNode());
		link1 = FilterBerlinKutter.network.getLinks().get(new IdImpl(1663));
		relevantFromNodes.add(link1.getFromNode());
		relevantToNodes.add(link1.getToNode());
		link1 = FilterBerlinKutter.network.getLinks().get(new IdImpl(1668));
		relevantFromNodes.add(link1.getFromNode());
		relevantToNodes.add(link1.getToNode());
	}

	@Override
	public void run(final Person person) {
		// check for selected plans routes, if any of the relevant nodes shows up
		Plan plan = person.getSelectedPlan();
		for (int jj = 0; jj < plan.getPlanElements().size(); jj++) {
			if (jj % 2 == 0) {
			}else {
				Leg leg = (Leg)plan.getPlanElements().get(jj);
				// route
				if (leg.getRoute() != null) {
					List<Node> nodes = ((CarRoute) leg.getRoute()).getNodes();
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
	public static Population relevantPopulation;
	public static NetworkLayer network;


	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		//String popFileName = "..\\..\\tmp\\studies\\berlin-wip\\kutter_population\\DSkutter010car_bln.router_wip.plans.v4.xml";
		String netFileName = "..\\..\\tmp\\studies\\berlin-wip\\network\\wip_net.xml";

		String popFileName = "..\\..\\tmp\\studies\\berlin-wip\\kutter_population\\30.plans.xml";
		String outpopFileName = "..\\..\\tmp\\studies\\berlin-wip\\kutter_population\\30_Jakob-Kaiser-RingONLY.plans.v4.xml";

		Gbl.startMeasurement();
		Gbl.createConfig(args);

		World world = Gbl.getWorld();

		network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFileName);
		world.setNetworkLayer(network);
		world.complete();

		relevantPopulation = new PopulationImpl(false);
		Population population = new MyPopulation();
		MatsimPopulationReader plansReader = new MatsimPopulationReader(population);
		population.addAlgorithm(new FilterPersons());
		plansReader.readFile(popFileName);
//		population.runAlgorithms();

		PopulationWriter plansWriter = new PopulationWriter(relevantPopulation, outpopFileName, "v4");
		plansWriter.write();
	}

}
