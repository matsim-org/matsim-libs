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
import java.util.Set;

import org.matsim.events.Events;
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.handler.LinkEnterEventHandler;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.queuesim.QueueSimulation;
import org.matsim.network.Link;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkWriter;
import org.matsim.network.Node;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.PopulationWriter;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.world.World;

class EventHH implements LinkEnterEventHandler {

	static Set<String> linkList = new HashSet<String>();
	
	public void handleEvent(LinkEnterEvent event) {
		linkList.add(event.linkId);
	}

	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}
	
}

class FilterPersons2 extends AbstractPersonAlgorithm{

	public static Set<Node> relevantFromNodes = new HashSet<Node>();
	public static Set<Node> relevantToNodes = new HashSet<Node>();

	int modulo = 1;
	int count = 0;
	
	public FilterPersons2(int modulo) {
		super();
		this.modulo = modulo;
	}

	@Override
	public void run(Person person) {
		// check for selected plans routes, if any of the relevant nodes shows up
		Plan plan = person.getSelectedPlan();
		if ((count++ % modulo) == 0) {
			try {
				ReducePopulationExe.relevantPopulation.addPerson(person);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}

public class ReducePopulationExe {
	public static Population relevantPopulation;
	public static NetworkLayer network;


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//String popFileName = "..\\..\\tmp\\studies\\berlin-wip\\kutter_population\\DSkutter010car_bln.router_wip.plans.v4.xml";
		String netFileName = "/TU Berlin/tmp/studies/ivtch/ivtch-osm.xml";
		String outnetFileName = "/TU Berlin/tmp/studies/ivtch/ivtch_red.xml";

		String popFileName = "/TU Berlin/tmp/studies/ivtch/plans100p.xml.gz";
		String outpopFileName = "/TU Berlin/tmp/studies/ivtch/plans50p.xml";

		Gbl.startMeasurement();
		Gbl.createConfig(args);

		World world = Gbl.getWorld();

		network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFileName);
		world.setNetworkLayer(network);

		relevantPopulation = new Population(false);
		Population population = new Population(true);
		MatsimPopulationReader plansReader = new MatsimPopulationReader(population);
		population.addAlgorithm(new FilterPersons2(2));
		plansReader.readFile(popFileName);

		System.out.println("read # persons: " );
		relevantPopulation.printPlansCount();
		population.runAlgorithms();
		
		PopulationWriter plansWriter = new PopulationWriter(relevantPopulation, outpopFileName, "v4");
		plansWriter.write();
		
		Events events = new Events();
		EventHH eventhh = new EventHH();
		
		events.addHandler(eventhh);
		QueueSimulation queueSim = new QueueSimulation(network, relevantPopulation, events);

		queueSim.run();
	
		Set<Link> nolinkList = new HashSet<Link>();
		for(Link link : network.getLinks().values()) if(!eventhh.linkList.contains(link.getId().toString())) nolinkList.add(link);
		
		for(Link link : nolinkList)network.removeLink(link);
		
		new NetworkWriter(network, outnetFileName).write();
		
	}

}
