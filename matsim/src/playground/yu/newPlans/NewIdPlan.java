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

import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Person;
import org.matsim.population.Population;

/**
 * @author yu
 * 
 */
public class NewIdPlan extends NewPlan {

	/**
	 * @param plans
	 */
	public NewIdPlan(final Population plans) {
		super(plans);
	}

	@Override
	public void run(final Person person) {
		if (Integer.parseInt(person.getId().toString()) <= 100)
			pw.writePerson(person);
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		Config config = Gbl.createConfig(args);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(config.network()
				.getInputFile());

		Population plans = new Population();
		NewIdPlan nip = new NewIdPlan(plans);
		plans.addAlgorithm(nip);
		new MatsimPopulationReader(plans, network).readFile(config.plans()
				.getInputFile());
		plans.runAlgorithms();
		nip.writeEndPlans();
	}
}
