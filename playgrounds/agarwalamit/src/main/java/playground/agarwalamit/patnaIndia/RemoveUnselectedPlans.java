/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.patnaIndia;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

/**
 *create new plans file which have only selected plans from input plans 
 * @author amit
 */
public class RemoveUnselectedPlans {

	public static void main(String[] args) {
		String clusterPath="/Users/aagarwal/Desktop/ils4/agarwal/patnaIndia/";
		String inputPlans = clusterPath+"/patnaOutput/modeChoice/run10/output_plans.xml.gz";
		String outputPlans = clusterPath+"/patnaOutput/modeChoice/selectedPlansOnly/plans.xml";

		Config config = ConfigUtils.createConfig();
		config.plans().setInputFile(inputPlans);
		Scenario sc= ScenarioUtils.loadScenario(config);

		Config config2 = ConfigUtils.createConfig();
		Scenario sc2 = ScenarioUtils.createScenario(config2);

		Population population = sc2.getPopulation();
		PopulationFactory factory = population.getFactory();

		System.out.println(sc.getPopulation().getPersons().size());

		for(Person p : sc.getPopulation().getPersons().values()){
			Plan selectedPlan = p.getSelectedPlan();

			//create a new person and new plan; new plan is same as selected plan.
			Person newP = factory.createPerson(p.getId());
			population.addPerson(newP);
			newP.addPlan(selectedPlan);

		}
		new PopulationWriter(population,sc.getNetwork()).write(outputPlans);
	}

}
