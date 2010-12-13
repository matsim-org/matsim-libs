/* *********************************************************************** *
 * project: org.matsim.*
 * PlanPTRoutesComparator.java
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
package playground.mmoyo.ptRouterAdapted.precalculation;

import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.population.LegImpl;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.api.core.v01.population.Plan;

import playground.mmoyo.utils.DataLoader;

public class PlanPTRoutesComparator {
	
	/**returns true if two plans have the same number of legs with same route description
	 * Useful to see if a new pre-calculated pt connection exists already 
	 * */
	public boolean haveSamePtRoutes(Plan plan1, Plan plan2){
		
		if (plan1.getPlanElements().size() != plan2.getPlanElements().size()){
			return false;
		}
	
		int i;
		for (i=0; i<plan1.getPlanElements().size();i++ ){
			PlanElement pe1 = plan1.getPlanElements().get(i);
			PlanElement pe2 = plan2.getPlanElements().get(i);
			
			if (pe1 instanceof LegImpl) {
				if (!(pe2 instanceof LegImpl)){
					return false;
				}
				GenericRouteImpl route1 =  (GenericRouteImpl) ((Leg)pe1).getRoute();
				GenericRouteImpl route2 =  (GenericRouteImpl) ((Leg)pe2).getRoute();
				
				if (!route1.getRouteDescription().equals(route2.getRouteDescription())){
					return false;
				}
			}
		}
		
		return true;
	}	

	public static void main(String[] args) {
		String popFile1 = "../playgrounds/mmoyo/output/precalculation/allRoutes.xml";
		DataLoader loader = new DataLoader();
		Population pop = loader.readPopulation(popFile1);
		
		Id person1Id = new IdImpl("11181922X4_cwalk4.0_dist0.5_tran120");
		Id person2Id = new IdImpl("11181922X4_cwalk4.0_dist0.5_tran180");
		
		Plan plan1 = pop.getPersons().get(person1Id).getSelectedPlan();
		Plan plan2 = pop.getPersons().get(person2Id).getSelectedPlan();

		System.out.println("have same routes: " + new PlanPTRoutesComparator().haveSamePtRoutes(plan1, plan2));
		
	}

}