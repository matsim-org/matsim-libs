/* *********************************************************************** *
 * project: org.matsim.*
 * DoublePtPlan.java
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

package playground.yu.newPlans;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;

/**
 * increases the amount of Agents in a new MATSim plansfile, by copying the old
 * agents in the file and change only the Ids.
 * 
 * @author ychen
 * 
 */
public class DoublePlan extends NewPopulation {
	// private String newPersonId;
	private Person tmpPerson = null;
	private int n = 0;

	/**
	 * Construcktor
	 * 
	 * @param plans
	 *            - a Plans Object, which derives from MATSim plansfile
	 */
	public DoublePlan(PopulationImpl plans, String filename) {
		super(plans, filename);
	}

	/**
	 * writes an old Person and also new Persons in new plansfile.
	 */
	@Override
	public void run(Person person) {

		pw.writePerson(person);
		// n++;
		tmpPerson = person;
		String oldId = person.getId().toString();
		// int oldId = Integer.parseInt(person.getId().toString());
		// produce new Person with new Id
		// if (n == 1) {
		for (int i = 1; i <= 89; i++)
			createNewPerson(oldId + i);
		// } else if (n == 2) {
		// for (int i = 1; i <= 89; i++)
		// createNewPerson(oldId+i);
		// }

		// createNewPerson(oldId + "A");
		// createNewPerson(oldId + "B");
		// createNewPerson(oldId + "C");
		// createNewPerson(oldId + "D");
	}

	private void createNewPerson(String newId) {
		tmpPerson.setId(new IdImpl(newId));
		pw.writePerson(tmpPerson);
	}

	public static void main(final String[] args) {
		Gbl.startMeasurement();

		String networkFilename = "../berlin-bvg09/pt/m2_schedule_delay/net.xml";
		String plansFilename = "../berlin-bvg09/pt/m2_schedule_delay/pop.xml";
		String outputPlansFilename = "../berlin-bvg09/pt/m2_schedule_delay/pop180.xml";

		ScenarioImpl s = new ScenarioImpl();

		NetworkLayer network = s.getNetwork();
		new MatsimNetworkReader(network).readFile(networkFilename);

		PopulationImpl population = s.getPopulation();
		PopulationReader plansReader = new MatsimPopulationReader(population,
				network);
		plansReader.readFile(plansFilename);

		DoublePlan dp = new DoublePlan(population, outputPlansFilename);
		dp.run(population);
		dp.writeEndPlans();

		Gbl.printElapsedTime();
	}
}
