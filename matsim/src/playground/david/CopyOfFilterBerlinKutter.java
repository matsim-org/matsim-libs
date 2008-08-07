/* *********************************************************************** *
 * project: org.matsim.*
 * CopyOfFilterBerlinKutter.java
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

import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.population.Leg;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.PopulationWriter;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.world.World;

class FilterPersonsNonCarMode extends AbstractPersonAlgorithm{

	public static Set<Node> relevantFromNodes = new HashSet<Node>();
	public static Set<Node> relevantToNodes = new HashSet<Node>();
	public int count = 0;

	public FilterPersonsNonCarMode() {
		super();
	}
	@Override
	public void run(Person person) {
		// check for selected plans routes, if any of the relevant nodes shows up
		Plan plan = person.getSelectedPlan();
		for (int jj = 0; jj < plan.getActsLegs().size(); jj++) {
			if (jj % 2 == 0) {
			} else {
				Leg leg = (Leg)plan.getActsLegs().get(jj);
				// route
				if (!leg.getMode().equals("car")  && (this.count < 10)) {
					try {
						CopyOfFilterBerlinKutter.relevantPopulation.addPerson(person);
						this.count++;
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}

}

public class CopyOfFilterBerlinKutter {
	public static Population relevantPopulation;
	public static NetworkLayer network;

	public static void main(String[] args) {
		//String popFileName = "..\\..\\tmp\\studies\\berlin-wip\\kutter_population\\DSkutter010car_bln.router_wip.plans.v4.xml";
		String netFileName = "..\\..\\tmp\\studies\\berlin-wip\\network\\wip_net.xml";

		String popFileName = "..\\..\\tmp\\studies\\berlin-wip\\kutter_population\\kutter001car5.debug.router_wip.plans.xml.gz";
		String outpopFileName = "..\\..\\tmp\\studies\\berlin-wip\\kutter_population\\10plans_with_non_car_mode.xml";

		Gbl.startMeasurement();
		Gbl.createConfig(args);

		World world = Gbl.getWorld();

		network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFileName);
		world.setNetworkLayer(network);

		relevantPopulation = new Population(false);
		Population population = new MyPopulation();
		MatsimPopulationReader plansReader = new MatsimPopulationReader(population);
		population.addAlgorithm(new FilterPersonsNonCarMode());
		plansReader.readFile(popFileName);
//		population.runAlgorithms();
		
		PopulationWriter plansWriter = new PopulationWriter(relevantPopulation, outpopFileName, "v4");
		plansWriter.write();
	}

}
