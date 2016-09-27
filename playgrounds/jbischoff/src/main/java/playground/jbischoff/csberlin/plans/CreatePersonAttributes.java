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

/**
 * 
 */
package playground.jbischoff.csberlin.plans;

import java.util.Random;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;


/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class CreatePersonAttributes {
	public static void main(String[] args) {
		Random r = MatsimRandom.getLocalInstance();
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile("C:/Users/Joschka/Documents/shared-svn/projects/bmw_carsharing/data/scenario/untersuchungsraum-plans.xml.gz");
		for (Person p : scenario.getPopulation().getPersons().values()){
			Integer age = (Integer) p.getCustomAttributes().get("age");
			String car = (String) p.getCustomAttributes().get("carAvail");
			Boolean license = ((String) p.getCustomAttributes().get("hasLicense")).equals("yes")?true:false;
			Boolean member = false;
			if (license){
				if (r.nextDouble()<0.28){
					member = true;
				}
			}
			if (age!=null)
			scenario.getPopulation().getPersonAttributes().putAttribute(p.getId().toString(), "age", age);
			if (car!=null)
			scenario.getPopulation().getPersonAttributes().putAttribute(p.getId().toString(), "carAvail", car);
			if(license!=null)
			scenario.getPopulation().getPersonAttributes().putAttribute(p.getId().toString(), "license", license);
			if (member!=null)
			scenario.getPopulation().getPersonAttributes().putAttribute(p.getId().toString(), "member", member);
			
		}
		new ObjectAttributesXmlWriter(scenario.getPopulation().getPersonAttributes()).writeFile("C:/Users/Joschka/Documents/shared-svn/projects/bmw_carsharing/data/scenario/untersuchungsraum-planscs50_oA.xml");
		
}
	
}
