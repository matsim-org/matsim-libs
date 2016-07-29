/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.wrashid.template;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

import playground.wrashid.tryouts.plan.NewPopulation;

/*
 * In KeepOnlyMIVPlans, you find an example where facilities are also involved...
 */

public class ProcessPlansFile extends NewPopulation {
	public static void main(String[] args) {

		MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		String inputPlansFile = "./test/scenarios/berlin/plans_hwh_1pct.xml.gz";
		String outputPlansFile = "./test.xml.gz";
		String networkFile = "./test/scenarios/berlin/network.xml.gz";

		Population inPop = sc.getPopulation();

		Network net = sc.getNetwork();
		new MatsimNetworkReader(sc.getNetwork()).readFile(networkFile);

		MatsimReader popReader = new PopulationReader(sc);
		popReader.readFile(inputPlansFile);

		ProcessPlansFile dp = new ProcessPlansFile(net, inPop, outputPlansFile);
		dp.run(inPop);
		dp.writeEndPlans();
	}

	public ProcessPlansFile(Network network, Population plans, String filename) {
		super(network, plans, filename);
	}

	@Override
	public void run(Person person) {
		this.popWriter.writePerson(person);

	}
}
