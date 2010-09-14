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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;

/**
 * increases the amount of Agents in a new MATSim plansfile, by copying the old
 * agents in the file and change only the Ids.
 * 
 * @author ychen
 * 
 */
public class DoublePop extends NewPopulation {
	// private String newPersonId;
	private Person tmpPerson = null;
	private final int n = 0;

	/**
	 * Construcktor
	 * 
	 * @param plans
	 *            - a Plans Object, which derives from MATSim plansfile
	 */
	public DoublePop(Network network, Population plans, String filename) {
		super(network, plans, filename);
	}

	/**
	 * writes an old Person and also new Persons in new plansfile.
	 */
	@Override
	public void run(Person person) {

		pw.writePerson(person);
		// n++;
		tmpPerson = person;
		// String oldId = person.getId().toString();
		long oldId = Integer.parseInt(person.getId().toString());
		// produce new Person with new Id
		// if (n == 1) {
		for (int i = 1; i < 2; i++)
		// createNewPerson(oldId + i);

		{
			createNewPerson(oldId + i * 100);
		}

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
		createNewPerson(new IdImpl(newId));
	}

	private void createNewPerson(long newId) {
		createNewPerson(new IdImpl(newId));
	}

	private void createNewPerson(Id newId) {
		tmpPerson.setId(newId);
		pw.writePerson(tmpPerson);
	}

	public static void main(final String[] args) {

		String networkFilename = "../../matsim/examples/equil/network.xml";
		String plansFilename = "../../MATSim_integration_demandCalibration/tests/plans100withPt.xml";
		String outputPlansFilename = "../../MATSim_integration_demandCalibration/tests/plans200withPt.xml";

		ScenarioImpl s = new ScenarioImpl();

		NetworkImpl network = s.getNetwork();
		new MatsimNetworkReader(s).readFile(networkFilename);

		Population population = s.getPopulation();
		PopulationReader plansReader = new MatsimPopulationReader(s);
		plansReader.readFile(plansFilename);

		DoublePop dp = new DoublePop(network, population, outputPlansFilename);
		dp.run(population);
		dp.writeEndPlans();
	}
}
