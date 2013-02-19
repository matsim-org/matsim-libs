/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.mmoyo.Validators;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;

/**
 * Validates that a given subPopulation is effectively a sample (subset) of another population 
 */
public class SubSetPopulationValidator {

	public SubSetPopulationValidator(){
		
	}
	
	protected int run (final Population pop, final Population subPop){
		int diffPersons=0;
		for (Person subPerson: subPop.getPersons().values()){
			Id subId = subPerson.getId();
			Person person = pop.getPersons().get(subId);
			
			if(person!=null){
				
				//validate that their selected plans have same activity types and leg transport mode
				Plan plan = person.getSelectedPlan();
				Plan subPlan = subPerson.getSelectedPlan();
				for (int i=0; i< plan.getPlanElements().size() ; i++){
					PlanElement pe = plan.getPlanElements().get(i);
					PlanElement subPe = plan.getPlanElements().get(i);
					
					if ((pe instanceof Activity)) {
						if (subPe instanceof Activity){
							
						}else{
							diffPersons++;	
							System.out.println(subId);
							continue;
						}
					
					}else{
						
					}
					
					
				}
				
				
			}else{
				diffPersons++;	
				System.out.println(subId);
			}
		}
		return diffPersons;
	}
	
	public static void main(String[] args) {

	}

}
