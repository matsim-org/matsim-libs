/* *********************************************************************** *
 * project: org.matsim.*
 * PlansExchanger.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.yu.newPlans;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;

/**
 * @author yu
 *
 */
public class PlansExchanger {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int n = 5;
		String netFilename = null;
		String[] oldPopFilenames, newPopFilenames;
		// String[] oldPopFilenames, newPopFilenames;
		if (args.length <= 0) {
			netFilename = "../matsim/examples/equil/network.xml";
			String popFilenameBase = "../matsim/output/equil/ITERS/it."//
			, postfix = ".plans.xml.gz";

			n = 5;

			oldPopFilenames = new String[n];
			newPopFilenames = new String[n];

			oldPopFilenames[0] = popFilenameBase + "60/60" + postfix;
			oldPopFilenames[1] = popFilenameBase + "70/70" + postfix;
			oldPopFilenames[2] = popFilenameBase + "80/80" + postfix;
			oldPopFilenames[3] = popFilenameBase + "90/90" + postfix;
			oldPopFilenames[4] = popFilenameBase + "100/100" + postfix;

			newPopFilenames[0] = popFilenameBase + "a" + postfix;
			newPopFilenames[1] = popFilenameBase + "b" + postfix;
			newPopFilenames[2] = popFilenameBase + "c" + postfix;
			newPopFilenames[3] = popFilenameBase + "d" + postfix;
			newPopFilenames[4] = popFilenameBase + "e" + postfix;

		} else if (args.length % 2 == 0/* even number */) {
			n = Integer.parseInt(args[0]);
			netFilename = args[1];
			oldPopFilenames = new String[n];
			newPopFilenames = new String[n];
			for (int i = 0; i < (args.length - 2) / 2; i++) {
				oldPopFilenames[i] = args[2 + i];
				newPopFilenames[i] = args[7 + i];
			}
		} else {
			netFilename = null;
			oldPopFilenames = null;
			newPopFilenames = null;
			System.err
					.println("The number of parameters should be an even number");
			System.exit(0);
		}

		List<Population> oldPops = new ArrayList<Population>()//
		, newPops = new ArrayList<Population>();

		// read old popFilenames
		for (int i = 0; i < oldPopFilenames.length; i++) {
			ScenarioImpl sc = new ScenarioImpl();
			NetworkImpl net = sc.getNetwork();
			new MatsimNetworkReader(sc).readFile(netFilename);

			Population pop = sc.getPopulation();
			new MatsimPopulationReader(sc).readFile(oldPopFilenames[i]);
			oldPops.add(pop);
			newPops.add(new PopulationImpl(sc));
		}

		for (Id personId : oldPops.get(0).getPersons().keySet()) {
			// only for the index of Persons
			for (int i = 0; i < newPopFilenames.length; i++) {
				// a new Person without plans
				PersonImpl personCopy = new PersonImpl(personId);
				// set the each "i."th plan of the persons from oldPops to the
				// person in newPop
				for (Population pop : oldPops) {
					personCopy.addPlan(pop.getPersons().get(personId)
							.getPlans().get(i));
				}
				newPops.get(i).addPerson(personCopy);
			}
		}

		ScenarioImpl sc = new ScenarioImpl();
		NetworkImpl net = sc.getNetwork();
		new MatsimNetworkReader(sc).readFile(netFilename);
		for (int i = 0; i < newPopFilenames.length; i++) {
			new PopulationWriter(newPops.get(i), net).write(newPopFilenames[i]);
		}
	}
}
