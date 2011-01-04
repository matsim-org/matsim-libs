/* *********************************************************************** *
 * project: org.matsim.*
 * ModeEnumerator.java
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
import java.util.List;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;

public class ModeEnumerator {
	Population population;
	
	public ModeEnumerator (Population population){
		this.population = population;
	}

	public void run(){
		List<String> modeList = new ArrayList<String>();
		for (Person person : this.population.getPersons().values()){
			for (Plan plan : person.getPlans()){
				for (PlanElement pe : plan.getPlanElements()){
					if ((pe instanceof Leg)) {
						String mode = ((Leg)pe).getMode();
						if (!modeList.contains(mode)){
							modeList.add(mode);
						}
					}
				}
			}
		}		
		
		//show modes
		for (String mode: modeList){
			System.out.println(mode);
		}
		
	}
		
	public static void main(String[] args) {
		String populationFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/inputPlans/doubleMinTransfersRoutes_plan2.xml.gz";
		Population population = new DataLoader().readPopulation(populationFile);
		new ModeEnumerator(population).run();
	}
	
}

