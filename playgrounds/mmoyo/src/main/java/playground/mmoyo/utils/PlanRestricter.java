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

package playground.mmoyo.utils;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.population.algorithms.PlansFilterByLegMode;

public class PlanRestricter {

	/**
	 * The opposite as playground.andreas.bln.pop.PlanExpander, this reads a expanded plan and take a sample of it, filters it to have only pt plans
	 */
	public static void main(String[] args) {
		String networkFile = "../shared-svn/studies/countries/de/berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";
		String plansFile = "../shared-svn/studies/countries/de/berlin-bvg09/pt/nullfall_berlin_brandenburg/input/baseplan_10x_subset_xy2links.xml.gz";

		Gbl.startMeasurement();

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network net = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(networkFile);

		Population population = scenario.getPopulation();
		PopulationReader popReader = new MatsimPopulationReader(scenario);
		popReader.readFile(plansFile);

		Gbl.startMeasurement();

		PlansFilterByLegMode plansFilter = new PlansFilterByLegMode( TransportMode.pt, PlansFilterByLegMode.FilterType.keepAllPlansWithMode) ;
		plansFilter.run(population) ;

        ScenarioImpl sc = ((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig()));
        Population newPopulation = PopulationUtils.createPopulation(sc.getConfig(), sc.getNetwork());
		for (Person person : population.getPersons().values()){
			String sId = person.getId().toString();

			if ( sId.indexOf("X")== -1 || sId.endsWith("X1") || sId.endsWith("X2") || sId.endsWith("X3") || sId.endsWith("X4")){
				newPopulation.addPerson(person);
			}
		}

		String outputFile = "../playgrounds/mmoyo/output/restrictedPlan.xml";
		System.out.println("writing output plan file..." + outputFile);
		PopulationWriter popwriter = new PopulationWriter(newPopulation, net);
		popwriter.write(outputFile) ;
		System.out.println("done");

		Gbl.printElapsedTime();
	}

}

