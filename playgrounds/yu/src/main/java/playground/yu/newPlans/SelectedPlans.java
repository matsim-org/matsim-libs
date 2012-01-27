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
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * writes a new Plansfile, in which every person will only have ONE {@code Plan}
 * , that also is the selected Plan of the old Plan.
 * 
 * @author ychen
 * 
 */
public class SelectedPlans extends NewPopulation {
	public static void main(final String[] args) {

		String netFilename, populationFilename, outputPopulationFilename;
		if (args.length == 3) {
			netFilename = args[0];
			populationFilename = args[1];
			outputPopulationFilename = args[2];
		} else {
			netFilename = "../matsimTests/ParamCalibration/network.xml";
			populationFilename = "../matsimTests/ParamCalibration/general2/reroute/ITERS/it.41/reroute.41.plans.xml.gz";
			outputPopulationFilename = "../matsimTests/ParamCalibration/general2/basePop.xml.gz";
		}

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils
				.createConfig());
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(netFilename);

		Population population = scenario.getPopulation();
		PopulationReader plansReader = new MatsimPopulationReader(scenario);
		plansReader.readFile(populationFilename);

		SelectedPlans sp = new SelectedPlans(network, population,
				outputPopulationFilename);
		sp.run(population);
		sp.writeEndPlans();
	}

	/**
	 * Constructor, writes file-head
	 * 
	 * @param plans
	 *            - a Plans Object, which derives from MATSim plansfile
	 */
	public SelectedPlans(final Network network, Population plans,
			String filename) {
		super(network, plans, filename);
	}

	@Override
	public void run(Person person) {
		Plan selectedPlan = person.getSelectedPlan();
		person.getPlans().clear();
		person.addPlan(selectedPlan);
		pw.writePerson(person);
	}
}
