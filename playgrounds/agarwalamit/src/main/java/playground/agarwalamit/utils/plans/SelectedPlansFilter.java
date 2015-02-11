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
package playground.agarwalamit.utils.plans;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * Take input plans and write selected plans from choice set of all person in a new file.
 * @author amit
 */
public class SelectedPlansFilter {

	public static Logger log = Logger.getLogger(SelectedPlansFilter.class);

	private final String runDir = "/Users/aagarwal/Desktop/ils4/agarwal/siouxFalls/output/run22/";
	private final String inputPlans = runDir + "/output_plans.xml.gz";

	private String outputPlans = runDir + "selectedPlansOnly.xml.gz"; 

	private void run (){
		Scenario sc = LoadMyScenarios.loadScenarioFromPlans(inputPlans);
		Scenario scOut = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population popOut = scOut.getPopulation();
		
		for (Person p :sc.getPopulation().getPersons().values()){
			Plan selectedPlan = p.getSelectedPlan();
			PopulationFactory factory = popOut.getFactory();
			Person newP = factory.createPerson(p.getId());
			popOut.addPerson(newP);
			newP.addPlan(selectedPlan);
		}
		new PopulationWriter(popOut).write(outputPlans);
		log.info("Writing selected plans only successful.");
	}

	public static void main(String[] args) {
		new SelectedPlansFilter().run();
	}
}
