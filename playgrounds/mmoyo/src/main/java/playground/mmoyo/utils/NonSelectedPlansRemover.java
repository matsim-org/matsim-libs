/* *********************************************************************** *
 * project: org.matsim.*
 * NonSelectedPlansRemover.java
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

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationWriter;

 /**
  *filters all plans leaving only the selected ones
  * @author manuel
  */
public class NonSelectedPlansRemover {

	public void writeOnlySelectedPlans(ScenarioImpl scenario){
		for (Person person : scenario.getPopulation().getPersons().values()){
			Collection <Plan> selectedPlanList = new ArrayList<Plan>();
			selectedPlanList.add(person.getSelectedPlan());
			person.getPlans().retainAll(selectedPlanList);
		}
		
		String outputFile = scenario.getConfig().controler().getOutputDirectory() + "/onlySelPlans.xml";
		System.out.println("writing output plan file..." + outputFile);
		PopulationWriter popwriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
		popwriter.write(outputFile) ;
		System.out.println("done");
	}
	
	public static void main(String[] args) {
		String configFile;

		if (args.length==1){
			configFile = args[0];
		}else{
			configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/100plans_bestValues_config.xml";
		}

		ScenarioImpl scenario = new TransScenarioLoader().loadScenario(configFile);
		new NonSelectedPlansRemover().writeOnlySelectedPlans(scenario);
	}

}
