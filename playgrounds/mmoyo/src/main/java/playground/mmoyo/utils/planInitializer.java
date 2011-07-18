/* *********************************************************************** *
 * project: org.matsim.*
 * planInitializer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
	
/**
 * Selected and unselected plans are converted into new initial plans, each new one becomes a numeric suffix. 
 * @author manuel
 */

public class planInitializer {
	Scenario scenario;
	
	public planInitializer(Scenario scenario){
		this.scenario = scenario;
	}
	
	public void run (){
		PopulationImpl initPopulation = new PopulationImpl(((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig())));
		
		final String SEPARATOR ="_";
		for (Person person : this.scenario.getPopulation().getPersons().values()){
			String strId = person.getId().toString();
			int suffix=0;
			for (Plan plan : person.getPlans()){
				Id initId= new IdImpl(strId + SEPARATOR + ++suffix);
				Person initPerson = new PersonImpl(initId);
				initPerson.addPlan(plan);
				initPopulation.addPerson(initPerson);
			}
		}
	
		String outputFile = this.scenario.getConfig().controler().getOutputDirectory() + "/initilizedPlans.xml";
		System.out.println("writing output plan file..." + outputFile);
		PopulationWriter popwriter = new PopulationWriter(initPopulation, this.scenario.getNetwork());
		popwriter.write(outputFile) ;
		System.out.println("done");
	}
	
	public static void main(String[] args) {
		String configFile;
		if (args.length>0){
			configFile = args[0];
		}else{
			configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/100plans_bestValues_config.xml";	
		}
		Scenario scenario = new DataLoader().loadScenario(configFile);
		new planInitializer(scenario).run();
	}

}
