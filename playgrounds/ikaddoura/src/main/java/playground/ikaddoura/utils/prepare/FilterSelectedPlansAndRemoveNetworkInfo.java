/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.utils.prepare;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import playground.agarwalamit.utils.LoadMyScenarios;

/**
* @author ikaddoura
*/

public class FilterSelectedPlansAndRemoveNetworkInfo {
	
	private static final Logger log = Logger.getLogger(FilterSelectedPlansAndRemoveNetworkInfo.class);
	
	private final static String inputPlans = "/Users/ihab/Documents/workspace/runs-svn/berlin_scenario_2016/be_122/be_122.output_plans.xml.gz";
	private final static String outputPlans = "/Users/ihab/Documents/workspace/runs-svn/cne/berlin-dz-10pct/input/be_122.output_plans_selected_noNetworkInfo.xml.gz";
	
	public static void main(String[] args) {
		
		FilterSelectedPlansAndRemoveNetworkInfo filter = new FilterSelectedPlansAndRemoveNetworkInfo();
		filter.run(inputPlans, outputPlans);
	}
	
	public void run (final String inputPlans, final String outputPlans) {
		
		Scenario scOutput;
		Scenario scInput = LoadMyScenarios.loadScenarioFromPlans(inputPlans);
		scOutput = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population popOutput = scOutput.getPopulation();
		
		for (Person p : scInput.getPopulation().getPersons().values()){
			Plan selectedPlan = p.getSelectedPlan();
			PopulationFactory factory = popOutput.getFactory();
			Person personNew = factory.createPerson(p.getId());
									
			popOutput.addPerson(personNew);
			
			// adjust plan
			for (PlanElement pE : selectedPlan.getPlanElements()) {
				if (pE instanceof Activity) {
					Activity act = (Activity) pE;
					act.setLinkId(null);
				}
				
				if (pE instanceof Leg) {
					Leg leg = (Leg) pE;
					leg.setRoute(null);
				}
				
			}
			
			
			personNew.addPlan(selectedPlan);
		}
		
		log.info("Writing population...");
		new PopulationWriter(scOutput.getPopulation()).write(outputPlans);
		log.info("Writing population... Done.");
	}

}

