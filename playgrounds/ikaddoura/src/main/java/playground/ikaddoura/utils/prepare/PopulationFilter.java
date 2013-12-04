/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.ikaddoura.utils.prepare;

import java.io.File;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author ikaddoura
 *
 */
public class PopulationFilter {
	
	private String outputPath = "/Users/ihab/Desktop/";
	private String inputPlansFile = "/Users/ihab/Desktop/output_plans.xml.gz";
	Scenario scenario_input;
	Scenario scenario_output;

	public static void main(String[] args) {
		
		PopulationFilter filter = new PopulationFilter();
		filter.run();		
	}

	private void run() {
		
		File directory = new File(this.outputPath);
		directory.mkdirs();
		
		Config config1 = ConfigUtils.createConfig();
		config1.plans().setInputFile(inputPlansFile);
		scenario_input = ScenarioUtils.loadScenario(config1);
		
		Config config2 = ConfigUtils.createConfig();
		scenario_output = ScenarioUtils.loadScenario(config2);
		
		filterSelectedCarPlans();
		
		PopulationWriter popWriter = new PopulationWriter(scenario_output.getPopulation(), null);
		popWriter.write("/Users/ihab/Desktop/selectedCarPlans.xml"); 
		
	}

	private void filterSelectedCarPlans() {
			
		for (Person person : scenario_input.getPopulation().getPersons().values()){
			Plan selectedPlan = person.getSelectedPlan();
			boolean planContainsCarLeg = false;
			
			List<PlanElement> planElements = selectedPlan.getPlanElements();
			for (int i = 0, n = planElements.size(); i < n; i++) {
				PlanElement pe = planElements.get(i);
				if (pe instanceof Leg) {
					Leg leg = (Leg) pe;
					if (leg.getMode().equals(TransportMode.car)){
						// leg has car mode
						planContainsCarLeg = true;
					} else {
							// no car mode
					}
				}
			}
			
			if (planContainsCarLeg){
				Person personCopy = scenario_output.getPopulation().getFactory().createPerson(person.getId());
				personCopy.addPlan(selectedPlan);
				scenario_output.getPopulation().addPerson(personCopy);
			}
		}
	}

}
