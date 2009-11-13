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

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;

/**
 * writes new Plansfile, in which every person will has 2 plans, one with type
 * "iv" and the other with type "oev", whose leg mode will be "pt" and who will
 * have only a blank <Route></Rout>
 * 
 * @author ychen
 * 
 */
public class NewSmallPlan extends NewPopulation {
	/**
	 * Constructor, writes file-head
	 * 
	 * @param plans
	 *            - a Plans Object, which derives from MATSim plansfile
	 */
	public NewSmallPlan(PopulationImpl plans) {
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
		final String inputPopFilename = "../runs-svn/run669/it.500/500.plans.xml.gz";
		final String outputPopFilename = "../matsimTests/Calibration/miniZurich/miniRun669it.500Plans.xml.gz";
		// new ScenarioLoader(
		// // "./test/yu/ivtch/config_for_10pctZuerich_car_pt_smallPlansl.xml"
		// // "../data/ivtch/make10pctPlans.xml"
		// "input/make10pctPlans.xml").loadScenario().getConfig();

		Scenario s = new ScenarioImpl();

		NetworkLayer network = (NetworkLayer) s.getNetwork();
		new MatsimNetworkReader(network).readFile(netFilename);

		PopulationImpl population = (PopulationImpl) s.getPopulation();
		Config c = s.getConfig();
		PlansConfigGroup pcg = c.plans();
		pcg.setOutputFile(outputPopFilename);
		pcg.setOutputSample(0.0001);

		NewSmallPlan nsp = new NewSmallPlan(population);
		PopulationReader plansReader = new MatsimPopulationReader(population,
				network);
		plansReader.readFile(inputPopFilename);

		nsp.run(population);
		nsp.writeEndPlans();
	}
}
