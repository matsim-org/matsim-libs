/* *********************************************************************** *
 * project: org.matsim.*
 * AvoidOldNodes.java
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

/**
 * 
 */
package playground.yu.newPlan;

import java.util.ArrayList;

import org.matsim.basic.v01.BasicLeg;
import org.matsim.basic.v01.BasicPlanImpl.LegIterator;
import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.world.World;

/**
 * @author yu
 * 
 */
public class AvoidOldNodes extends NewPlan {

	/**
	 * 
	 */
	public AvoidOldNodes(Plans plans) {
		super(plans);
	}

	@Override
	public void run(Person person) {
		for (Plan p : person.getPlans()) {
			for (LegIterator i = p.getIteratorLeg(); i.hasNext();) {
				BasicLeg bl = i.next();
				tag: {
					for (final Node n : (ArrayList<Node>) (bl.getRoute()
							.getRoute())) {
						final String nId = n.getId().toString();
						if (nId.equals("2513") || nId.equals("3226")) {
							break;
						}
					}
				}
				bl.setRoute(null);
			}
		}
		this.pw.writePerson(person);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		World world = Gbl.getWorld();
		Config config = Gbl.createConfig(args);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(config.network()
				.getInputFile());
		world.setNetworkLayer(network);

		Plans population = new Plans();
		AvoidOldNodes aon = new AvoidOldNodes(population);
		population.addAlgorithm(aon);
		new MatsimPlansReader(population).readFile(config.plans()
				.getInputFile());
		world.setPopulation(population);
		population.runAlgorithms();
		aon.writeEndPlans();
	}

}
