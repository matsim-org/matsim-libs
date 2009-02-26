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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.matsim.events.LinkEnterEvent;
import org.matsim.events.handler.LinkEnterEventHandler;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.BasicLeg;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.CarRoute;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Node;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
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
	}
	
}

class FilterPersons2 extends AbstractPersonAlgorithm{

	public static Set<Node> relevantFromNodes = new HashSet<Node>();
	public static Set<Node> relevantToNodes = new HashSet<Node>();

	int modulo = 1;
	int count = 0;
	PopulationWriter plansWriter;
	public Set<Link> usedlinkList = new HashSet<Link>();

	
	public FilterPersons2(int modulo, PopulationWriter plansWriter) {
		super();
		this.modulo = modulo;
		this.plansWriter = plansWriter;
	}

	public void addLinks(Plan p) {
		List<?> actl = p.getActsLegs();
		for (int i= 0; i< actl.size() ; i++) {
				if (i % 2 == 0) {
					// activity
					Act a = (Act)actl.get(i);
					this.usedlinkList.add(a.getLink());
				} else {
					// Leg
					Leg l = (Leg) actl.get(i);
					List<Link> ll = new LinkedList<Link>();
					for(Link link : ((CarRoute) l.getRoute()).getLinks()) {
						usedlinkList.add(link);
					}
				}
		}
		
	}
	@Override
	public void run(Person person) {
		// check for selected plans routes, if any of the relevant nodes shows up
		person.removeUnselectedPlans();
		Plan plan = person.getSelectedPlan();
		Leg leg = plan.getNextLeg(plan.getFirstActivity());
		if(!leg.getMode().equals(BasicLeg.Mode.car)) {
			System.out.print("X");
			return;
		}
		if ((count++ % modulo) == 0) {
			try {
				plansWriter.writePerson(person);
				//addLinks(plan);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if(count % 10000 == 0) {
			System.out.println("");
			System.out.println("Count == " + count);
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
		final int b;
		b=324;
		
		//String popFileName = "..\\..\\tmp\\studies\\berlin-wip\\kutter_population\\DSkutter010car_bln.router_wip.plans.v4.xml";
		//String netFileName = "../../tmp/studies/ivtch/ivtch-osm.xml";
		String netFileName = "../../tmp/studies/ivtch/ivtch_red100.xml";
		String popFileName = "../../tmp/studies/ivtch/plans10p.xml";
		String outpopFileName = "../../tmp/studies/ivtch/plans1p.xml";

		Gbl.startMeasurement();
		Gbl.createConfig(args);

		World world = Gbl.getWorld();

		network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFileName);
		world.setNetworkLayer(network);
		world.complete();

		relevantPopulation = new Population(Population.USE_STREAMING);
		PopulationWriter plansWriter = new PopulationWriter(relevantPopulation, outpopFileName, "v4");

		Population population = new Population(Population.USE_STREAMING);
		MatsimPopulationReader plansReader = new MatsimPopulationReader(population);
		FilterPersons2 filter = new FilterPersons2(10, plansWriter);
		population.addAlgorithm(filter);
		plansReader.readFile(popFileName);

		System.out.println("write # persons: " );
		relevantPopulation.printPlansCount();
		population.runAlgorithms();
		
		plansWriter.writeEndPlans();
		
//		List<Link> nolinkList = new LinkedList<Link>();
//		for(Link link : network.getLinks().values()) if(!filter.usedlinkList.contains(link)) nolinkList.add(link);
//		
//		for(Link link : nolinkList)network.removeLink(link);
//		
//		new NetworkWriter(network, outnetFileName).write();
		
	}

}
