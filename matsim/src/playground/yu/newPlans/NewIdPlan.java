/* *********************************************************************** *
 * project: org.matsim.*
 * newIdPlan.java
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
package playground.yu.newPlans;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;

/**
 * @author yu
 * 
 */
public class NewIdPlan extends NewPopulation {

	/**
	 * @param plans
	 */
	public NewIdPlan(final PopulationImpl plans) {
		super(plans);
	}

	@Override
	public void run(final Person person) {
		if (Integer.parseInt(person.getId().toString()) <= 100)
			this.pw.writePerson(person);
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		Config config = new ScenarioLoaderImpl(args[0]).loadScenario().getConfig();

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(config.network()
				.getInputFile());

		PopulationImpl plans = new PopulationImpl();
		NewIdPlan nip = new NewIdPlan(plans);
		new MatsimPopulationReader(plans, network).readFile(config.plans()
				.getInputFile());
		nip.run(plans);
		nip.writeEndPlans();
	}
}
