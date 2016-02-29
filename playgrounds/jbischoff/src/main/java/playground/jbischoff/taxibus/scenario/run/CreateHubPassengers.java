/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.jbischoff.taxibus.scenario.run;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

import playground.jbischoff.taxibus.algorithm.utils.TaxibusUtils;

/**
 * @author  jbischoff
 *
 */
public class CreateHubPassengers {
	public static void main(String[] args) {
	String dir = "../../../shared-svn/projects/vw_rufbus/scenario/input/";
	String inputPlans = dir + "vw060TB.output_plans.xml.gz";
	Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	Population newpop = PopulationUtils.createPopulation(ConfigUtils.createConfig());
	new MatsimPopulationReader(scenario).readFile(inputPlans);
	
	for (Person p : scenario.getPopulation().getPersons().values()){
		Plan plan = p.getSelectedPlan();
		Person newPerson = PopulationUtils.createPerson(p.getId());
		newPerson.addPlan(plan);
		newpop.addPerson(newPerson);
		if (p.getId().toString().startsWith("BS_WB")){
			PlanElement pE = plan.getPlanElements().get(1) ;
			if (pE instanceof Leg){
				if (((Leg) pE).getMode().equals(TaxibusUtils.TAXIBUS_MODE)){
					((Leg) pE).setRoute(null);
					Activity hub = newpop.getFactory().createActivityFromLinkId("tb_hub", Id.createLinkId(57195));
//					Activity hub = newpop.getFactory().createActivityFromLinkId("tb_hub", Id.createLinkId(15073));
					hub.setMaximumDuration(60);
					Leg newleg = newpop.getFactory().createLeg(TaxibusUtils.TAXIBUS_MODE);
					plan.getPlanElements().add(2, hub);
					plan.getPlanElements().add(3, newleg);
					
				}
			}
		}
		
		
		
		
	}
	new PopulationWriter(newpop).write(dir+"hubPlansTB060.xml.gz");
	
	
	
		
		
	}
}
