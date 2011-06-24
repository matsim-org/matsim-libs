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
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

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

		// for (int i = 1; i < 20; i++)
		//
		// {
		// createNewPerson(oldId + i * 5);
		// }

		// } else if (n == 2) {

		for (int i = 1; i <= 99; i++) {
			createAndWriteNewPerson(oldId + i);
		}

		// createNewPerson(oldId + "A");
		// createAndWriteNewPerson(oldId + "B");
		// createNewPerson(oldId + "C");
		// createNewPerson(oldId + "D");
	}

	private void createAndWriteNewPerson(String newId) {
		createAndWriteNewPerson(new IdImpl(newId));
	}

	private void createAndWriteNewPerson(long newId) {
		createAndWriteNewPerson(new IdImpl(newId));
	}

	private void createAndWriteNewPerson(Id newId) {
		tmpPerson.setId(newId);
		pw.writePerson(tmpPerson);
	}

	public static void main(final String[] args) {

		String networkFilename, plansFilename, outputPlansFilename;

		if (args.length != 3) {
			networkFilename = "test/input/2routes/network.xml";
			plansFilename = "test/input/2routes/plan.xml";
			outputPlansFilename = "test/input/2routes/pop100.xml";
		} else {
			networkFilename = args[0];
			plansFilename = args[1];
			outputPlansFilename = args[2];
		}

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils
				.createScenario(ConfigUtils.createConfig());

		new MatsimNetworkReader(scenario).readFile(networkFilename);

		Population population = scenario.getPopulation();
		new MatsimPopulationReader(scenario).readFile(plansFilename);

		DoublePop dp = new DoublePop(scenario.getNetwork(), population,
				outputPlansFilename);
		dp.run(population);
		dp.writeEndPlans();
	}
}
