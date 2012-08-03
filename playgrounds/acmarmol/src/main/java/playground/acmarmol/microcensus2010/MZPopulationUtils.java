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

package playground.acmarmol.microcensus2010;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.utils.objectattributes.ObjectAttributes;

/**
* 
* Helper class to filter the population
* 
*
* @author acmarmol
* 
*/

public class MZPopulationUtils {
	
//////////////////////////////////////////////////////////////////////
//member variables
//////////////////////////////////////////////////////////////////////

private static final String HOME = "home";	
private static final String WORK = "work";	
	

//////////////////////////////////////////////////////////////////////
//public methods
//////////////////////////////////////////////////////////////////////	

	public static void removePlans(final Population population, final Set<Id> ids) {
		for (Id id : ids) {
			Person p = population.getPersons().remove(id);
			if (p == null) { Gbl.errorMsg("pid="+id+": id not found in the plans DB!"); }
		}
	}



//////////////////////////////////////////////////////////////////////

	public static Set<Id> identifyPlansWithoutActivities(final Population population) {
		Set<Id> ids = new HashSet<Id>();
		for (Person person : population.getPersons().values()) {
			if(person.getSelectedPlan()==null){
			ids.add(person.getId());}
		}
		return ids;
	}


//////////////////////////////////////////////////////////////////////
	
	public static Set<Id> identifyNonHomeBasedPlans(final Population population) {
		Set<Id> ids = new HashSet<Id>();
		for (Person p : population.getPersons().values()) {
			Plan plan = p.getSelectedPlan();
			ActivityImpl last = (ActivityImpl)plan.getPlanElements().get(plan.getPlanElements().size()-1);
			if (!last.getType().equals(HOME)) { ids.add(p.getId()); }
		}
		return ids;
	}

//////////////////////////////////////////////////////////////////////

	public static Set<Id> identifyPlansWithNegCoords(final Population population) {
		Set<Id> ids = new HashSet<Id>();
		for (Person person : population.getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof ActivityImpl) {
				ActivityImpl act = (ActivityImpl) pe;
				if ((act.getCoord().getX()<0) || (act.getCoord().getY()<0)) { ids.add(person.getId()); }
				}
			}
		}
		return ids;
	}	

//////////////////////////////////////////////////////////////////////

	public static Set<Id> identifyPlansWithTooLongWalkTrips(final Population population) {
	Set<Id> ids = new HashSet<Id>();
	for (Person person : population.getPersons().values()) {
		Plan plan = person.getSelectedPlan();
		if(plan!=null){ //avoid persons without activities
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Leg) {
					Leg leg = (Leg) pe;
					if ((leg.getMode().equals(TransportMode.walk))&&(leg.getRoute().getDistance()>10000.0)) {ids.add(person.getId()); }
				}
			}
		}
	}
	return ids;
}	
	
//////////////////////////////////////////////////////////////////////
	
	public static void setHomeLocations(final Population population, final ObjectAttributes householdAttributes, final ObjectAttributes populationAttributes) {
		int counter = 0;
		for (Person person : population.getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			String hhnr = (String) populationAttributes.getAttribute(person.getId().toString(), "household number");
			CoordImpl homeCoord = (CoordImpl)householdAttributes.getAttribute(hhnr, "coord");

			if(plan!=null){ //avoid persons without activities
				for (int i=0; i<plan.getPlanElements().size(); i=i+2) {
					Activity act = (ActivityImpl)plan.getPlanElements().get(i);
					if ((act.getCoord().getX() == homeCoord.getX()) && (act.getCoord().getY() == homeCoord.getY())) {
						if (!act.getType().equals(HOME)) {
							act.setType(HOME);
							counter++;
	//						System.out.println("        pid=" + p.getId() + "; act_nr=" + (i/2) + ": set type to '"+HOME+"'");
						}
					}
				}
			}
		}
		System.out.println("      Number of activities set to home: " + counter);
	}

//////////////////////////////////////////////////////////////////////	
	
	public static void setWorkLocations(final Population population, final ObjectAttributes populationAttributes) {
		int counter = 0;
		for (Person person : population.getPersons().values()) {
		
			if(((PersonImpl) person).isEmployed()){
				
				Plan plan = person.getSelectedPlan();
				CoordImpl workCoord = (CoordImpl)populationAttributes.getAttribute(person.getId().toString(), "work: location coord");
				
				if(plan!=null){ //avoid persons without activities
					for (int i=0; i<plan.getPlanElements().size(); i=i+2) {
						Activity act = (ActivityImpl)plan.getPlanElements().get(i);
						if ((act.getCoord().getX() == workCoord.getX()) && (act.getCoord().getY() == workCoord.getY())) {
							if (!act.getType().equals(WORK)) {
							act.setType(WORK);
							counter++;
							//						System.out.println("        pid=" + p.getId() + "; act_nr=" + (i/2) + ": set type to '"+HOME+"'");
							}
						}
					}
				}
			}
		}	
		System.out.println("      Number of activities set to work: " + counter);
	}

//////////////////////////////////////////////////////////////////////
	

public static Set<Id> identifyPlansWithUndefinedNegCoords(final Population population) {
	Set<Id> ids = new HashSet<Id>();
		for (Person person : population.getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			if(plan!=null){ //avoid persons without activities
					for (PlanElement pe : plan.getPlanElements()) {
					if (pe instanceof ActivityImpl) {
					ActivityImpl act = (ActivityImpl) pe;
						if (((act.getCoord().getX() == -97) || (act.getCoord().getY() == -97))) {
							ids.add(person.getId());
							}
					}
				}
			}
		}
	return ids;
}	
		
//////////////////////////////////////////////////////////////////////

public static Set<Id> identifyPlansWithoutBestPrecision(final Population population) {
	Set<Id> ids = new HashSet<Id>();
	for (Person person : population.getPersons().values()) {	
		Plan plan = person.getSelectedPlan();
		if(plan!=null){ //avoid persons without activities
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Leg) {
					Leg leg = (Leg) pe;
					if (leg.getRoute().getDistance() == -99000) { ids.add(person.getId()); }
				}
			}
		}
	}
	return ids;
}	
	
	
	
	
	
	
	
}
