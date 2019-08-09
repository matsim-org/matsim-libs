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

package wobscenario.plans;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

/**
 * @author  jbischoff
 *
 */
public class CreateTaxibusSubpopulation {

	private void run(){
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile("C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/scenario/input/VW79BC.output_plans.xml.gz");
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
			PopulationUtils.putPersonAttribute(p, "subpopulation", "taxibusCustomer");
		}
		}
//		new ObjectAttributesXmlWriter(scenario.getPopulation().getPersonAttributes()).writeFile("C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/scenario/input/bswb_pesonAttributes.xml");
        new PopulationWriter(scenario.getPopulation()).write("C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/scenario/input/bswb_pesonAttributes.xml"); //not sure if this is what was originally intended here..
    }

	public static void main(String[] args) {
		CreateTaxibusSubpopulation s = new CreateTaxibusSubpopulation();
		s.run();
		
	}

}
