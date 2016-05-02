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

package playground.jbischoff.taxibus.scenario.plans;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

/**
 * @author  jbischoff
 *
 */
public class CreateTaxibusSubpopulation {

	private void run(){
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(scenario).readFile("C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/scenario/input/VW79BC.output_plans.xml.gz");
		for (Person p : scenario.getPopulation().getPersons().values()){
			boolean rep = false;
			if (p.getId().toString().startsWith("BS_WB")||p.getId().toString().startsWith("WB_BS")){
			Plan plan = p.getSelectedPlan();
			
			int actCounter = 0;
			for (PlanElement pE : plan.getPlanElements()){
				if (pE instanceof Activity){
					if (((Activity) pE).getType().equals("pt interaction")) continue;
					actCounter++;
					if (actCounter==3){
						if (((Activity) pE).getType().equals("home")) rep = true; 
					}
				}
			}
			}
		if (rep){
			scenario.getPopulation().getPersonAttributes().putAttribute(p.getId().toString(), "subpopulation", "taxibusCustomer");
		}
		
		}
		new ObjectAttributesXmlWriter(scenario.getPopulation().getPersonAttributes()).writeFile("C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/scenario/input/bswb_pesonAttributes.xml");
	}
	
	public static void main(String[] args) {
		CreateTaxibusSubpopulation s = new CreateTaxibusSubpopulation();
		s.run();
		
	}

}
