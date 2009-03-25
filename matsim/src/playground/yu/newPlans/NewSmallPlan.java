/* *********************************************************************** *
 * project: org.matsim.*
 * NewAgentPtPlan.java
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

import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.PersonAlgorithm;
import org.matsim.core.api.population.Population;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.PopulationImpl;
import org.matsim.population.PopulationReader;

/**
 * writes new Plansfile, in which every person will has 2 plans, one with type
 * "iv" and the other with type "oev", whose leg mode will be "pt" and who will
 * have only a blank <Route></Rout>
 * 
 * @author ychen
 * 
 */
public class NewSmallPlan extends NewPopulation implements PersonAlgorithm {
	/**
	 * Constructor, writes file-head
	 * 
	 * @param plans -
	 *            a Plans Object, which derives from MATSim plansfile
	 */
	public NewSmallPlan(Population plans) {
		super(plans);
	}

	@Override
	public void run(Person person) {
		// if (Math.random() < 0.12) {
		pw.writePerson(person);
		// }
	}
	public static void main(final String[] args) {
		// final String netFilename = "./test/yu/ivtch/input/network.xml";
		final String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		final String plansFilename = "../schweiz-ivtch-SVN/baseCase/plans/plans_all_zrh30km_transitincl_10pct.xml.gz";

		Gbl.createConfig(new String[] {
		// "./test/yu/ivtch/config_for_10pctZuerich_car_pt_smallPlansl.xml"
				// "../data/ivtch/make10pctPlans.xml"
				"input/make10pctPlans.xml" });

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);
		Population population = new PopulationImpl();
		NewSmallPlan nsp = new NewSmallPlan(population);

		PopulationReader plansReader = new MatsimPopulationReader(population,
				network);
		plansReader.readFile(plansFilename);
		nsp.run(population);
		nsp.writeEndPlans();
	}
}
