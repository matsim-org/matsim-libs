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

package playground.wrashid.lib.tools.facility;

import java.util.HashSet;
import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;


public class RemoveUnusedFacilities {

	public static void main(String[] args) {
		String inputPlansFile="C:/data/parkingSearch/zurich/input/plans.xml.gz";
		String inputNetworkFile="C:/data/parkingSearch/zurich/input/network.xml.gz";
		String inputFacilities="C:/data/parkingSearch/zurich/input/facilities.xml.gz";
		
		
		String outputFacilitiesFile="C:/data/parkingSearch/zurich/input/trimmed_facilities.xml.gz";	
	
		ScenarioImpl scenario= (ScenarioImpl) GeneralLib.readScenario(inputPlansFile, inputNetworkFile,inputFacilities);
		
		HashSet<Id> usedFacilities=new HashSet<Id>();
		
		
		for (Person person:scenario.getPopulation().getPersons().values()){
			for (Plan plan:person.getPlans()){
				for (PlanElement pe:plan.getPlanElements()){
					if (pe instanceof Activity){
						ActivityImpl activity=(ActivityImpl) pe;
						usedFacilities.add(activity.getFacilityId());
					}
				}
			}
		}
		
		ActivityFacilitiesImpl activityFacilities = (ActivityFacilitiesImpl) scenario.getActivityFacilities();
		LinkedList<Id> notUsedFacilities=new LinkedList<Id>();
		for (ActivityFacility facility:activityFacilities.getFacilities().values()){
			if (!usedFacilities.contains(facility.getId())){
				notUsedFacilities.add(facility.getId());
				
			}
		}
		
		for (Id facilityId:notUsedFacilities){
			activityFacilities.getFacilities().remove(facilityId);
		}
		
		
		
		GeneralLib.writeActivityFacilities(activityFacilities, outputFacilitiesFile);
	}
	
}
