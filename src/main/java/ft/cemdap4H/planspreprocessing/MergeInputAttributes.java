/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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
package ft.cemdap4H.planspreprocessing;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class MergeInputAttributes {

	public static void main(String[] args) {
		String attributesPersonFile = "C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/projekt2/data/new_cemdap_scenario/100_test/plans.xml.gz";
//		String personsFile = "D:/cemdap-vw/cemdap_output/mergedplans_filtered_0.1.xml.gz";
//		String personsFile = "D:/cemdap-vw/cemdap_output/mergedplans_filtered_1.0.xml.gz";
		String personsFile = "D:/cemdap-vw/cemdap_output/mergedplans_filtered_0.01.xml.gz";
		new MergeInputAttributes().run(personsFile, attributesPersonFile);
	}
	public void run(String personsFile, String attributesPersonFile){
		Scenario scenAtt = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Scenario scenPop = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		new PopulationReader(scenAtt).readFile(attributesPersonFile);
		new PopulationReader(scenPop).readFile(personsFile);
		
		for (Person p : scenPop.getPopulation().getPersons().values()){
			Person pAtt = scenAtt.getPopulation().getPersons().get(p.getId());
			if (pAtt!=null){
				PersonUtils.setAge(p, PersonUtils.getAge(pAtt));
				Boolean license = (Boolean) pAtt.getAttributes().getAttribute("hasLicense");
				if (license){
					PersonUtils.setLicence(p, "yes");
				} else {
					PersonUtils.setLicence(p, "no");
				}
				String schoolLoc = (String) pAtt.getAttributes().getAttribute("locationOfSchool");
				if (!schoolLoc.equals("-99")){
					PersonUtils.setLicence(p, "no");
					for (Plan plan : p .getPlans()){
						for (PlanElement pe : plan.getPlanElements()){
							if (pe instanceof Activity){
								Activity act = (Activity) pe;
								if(act.getType().startsWith("work")){
									act.setType("education");
								}
							}
						}
					}
				}
				PersonUtils.setEmployed(p, PersonUtils.isEmployed(pAtt));
				int genderBit = (Integer) pAtt.getAttributes().getAttribute("gender");
				if (genderBit == 0) PersonUtils.setSex(p, "male"); else PersonUtils.setSex(p, "female");
			
				if (PersonUtils.getLicense(p).equals("no")){
					PersonUtils.setCarAvail(p, "never");
					for (Plan plan : p .getPlans()){
						for (PlanElement pe : plan.getPlanElements()){
							if (pe instanceof Leg){
								if (((Leg) pe).getMode().equals(TransportMode.car)){
									((Leg) pe).setMode(TransportMode.ride);
								}
							}
						}
					}
				} else {
					PersonUtils.setCarAvail(p, "always");
					
				}
			
			}
			
			
			
		}
		new PopulationWriter(scenPop.getPopulation()).write(personsFile);
		
	}
}
