/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.vsp.andreas.utils.pop;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * Create numberOfcopies additional persons for a given plan.
 *
 * @author aneumann
 *
 */
public class DuplicatePlans extends NewPopulation {

	private final int numberOfCopies;

	public DuplicatePlans(Network network, Population plans, String filename, int numberOfCopies) {
		super(network, plans, filename);
		this.numberOfCopies = numberOfCopies;
	}

	@Override
	public void run(Person person) {
		// Keep old person untouched
		this.popWriter.writePerson(person);
		Id<Person> personId = person.getId();

		for (int i = 1; i < this.numberOfCopies + 1; i++) {

            PopulationUtils.changePersonId( ((Person) person), Id.create(personId.toString() + "X" + i, Person.class) ) ;
            this.popWriter.writePerson(person);

		}

	}

	public static void main(final String[] args) {
		Gbl.startMeasurement();

		MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		String networkFile = "./bb_cl.xml.gz";
		String inPlansFile = "./plan_korridor.xml.gz";
		String outPlansFile = "./plan_korridor_50x.xml.gz";

		Network net = sc.getNetwork();
		new MatsimNetworkReader(sc.getNetwork()).readFile(networkFile);

		Population inPop = sc.getPopulation();
		MatsimReader popReader = new PopulationReader(sc);
		popReader.readFile(inPlansFile);

		DuplicatePlans dp = new DuplicatePlans(net, inPop, outPlansFile, 49);
		dp.run(inPop);
		dp.writeEndPlans();

		Gbl.printElapsedTime();
	}
}
