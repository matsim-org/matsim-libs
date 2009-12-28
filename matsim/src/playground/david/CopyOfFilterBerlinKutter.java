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

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

class FilterPersonsNonCarMode extends AbstractPersonAlgorithm{

	public static Set<NodeImpl> relevantFromNodes = new HashSet<NodeImpl>();
	public static Set<NodeImpl> relevantToNodes = new HashSet<NodeImpl>();
	public int count = 0;

	public FilterPersonsNonCarMode() {
		super();
	}
	@Override
	public void run(final Person person) {
		// check for selected plans routes, if any of the relevant nodes shows up
		Plan plan = person.getSelectedPlan();
		for (int jj = 0; jj < plan.getPlanElements().size(); jj++) {
			if (jj % 2 == 0) {
			} else {
				Leg leg = (Leg)plan.getPlanElements().get(jj);
				// route
				if (!leg.getMode().equals(TransportMode.car)  && (this.count < 10)) {
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
	public static PopulationImpl relevantPopulation;
	public static NetworkLayer network;

	public static void main(final String[] args) {
		//String popFileName = "..\\..\\tmp\\studies\\berlin-wip\\kutter_population\\DSkutter010car_bln.router_wip.plans.v4.xml";
		String netFileName = "..\\..\\tmp\\studies\\berlin-wip\\network\\wip_net.xml";

		String popFileName = "..\\..\\tmp\\studies\\berlin-wip\\kutter_population\\kutter001car5.debug.router_wip.plans.xml.gz";
		String outpopFileName = "..\\..\\tmp\\studies\\berlin-wip\\kutter_population\\10plans_with_non_car_mode.xml";

		Gbl.startMeasurement();
		Gbl.createConfig(args);

		network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFileName);

		relevantPopulation = new PopulationImpl();
		PopulationImpl population = new MyPopulation();
		MatsimPopulationReader plansReader = new MatsimPopulationReader(population, network);
//		population.addAlgorithm(new FilterPersonsNonCarMode());
		plansReader.readFile(popFileName);
//		population.runAlgorithms();

		new PopulationWriter(relevantPopulation).writeFile(outpopFileName);
	}

}
