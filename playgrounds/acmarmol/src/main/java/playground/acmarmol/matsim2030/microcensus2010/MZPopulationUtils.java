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

package playground.acmarmol.matsim2030.microcensus2010;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.utils.objectattributes.ObjectAttributes;

import playground.acmarmol.utils.MyCollectionUtils;

import com.google.common.base.Supplier;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
* 
* Helper class with methods to filter/handle the population
* 
*
* @author acmarmol
* 
*/

public class MZPopulationUtils {
	
//////////////////////////////////////////////////////////////////////
//member variables
//////////////////////////////////////////////////////////////////////
	
	private final static Logger log = Logger.getLogger(MZPopulationUtils.class);

//////////////////////////////////////////////////////////////////////
//public methods
//////////////////////////////////////////////////////////////////////	

	public static void removePlans(final Population population, final Set<Id<Person>> ids) {
		for (Id<Person> id : ids) {
			Person p = population.getPersons().remove(id);
			if (p == null) { throw new RuntimeException("pid="+id+": id not found in the plans DB!"); }
		}
	}



//////////////////////////////////////////////////////////////////////

	public static Set<Id<Person>> identifyPlansWithoutActivities(final Population population) {
		Set<Id<Person>> ids = new HashSet<Id<Person>>();
		for (Person person : population.getPersons().values()) {
			if(person.getSelectedPlan()==null){
			ids.add(person.getId());}
		}
		return ids;
	}


//////////////////////////////////////////////////////////////////////
	
	public static Set<Id<Person>> identifyNonHomeBasedPlans(final Population population) {
		Set<Id<Person>> ids = new HashSet<>();
		for (Person p : population.getPersons().values()) {
			Plan plan = p.getSelectedPlan();
			ActivityImpl last = (ActivityImpl)plan.getPlanElements().get(plan.getPlanElements().size()-1);
			ActivityImpl first = (ActivityImpl)plan.getPlanElements().get(0);
			if (!last.getType().equals(MZConstants.HOME) | !first.getType().equals(MZConstants.HOME) ) { ids.add(p.getId()); }
		}
		return ids;
	}
	
//////////////////////////////////////////////////////////////////////
	
	public static Set<Id<Person>> identifyNonRoundPlans(final Population population) {
		Set<Id<Person>> ids = new HashSet<>();
		for (Person p : population.getPersons().values()) {
			Plan plan = p.getSelectedPlan();
			ActivityImpl last = (ActivityImpl)plan.getPlanElements().get(plan.getPlanElements().size()-1);
			ActivityImpl first = (ActivityImpl)plan.getPlanElements().get(0);
			if (!last.getType().equals(first.getType())) { ids.add(p.getId()); }
		}
		return ids;
	}

//////////////////////////////////////////////////////////////////////

	public static Set<Id<Person>> identifyPlansWithNegCoords(final Population population) {
		Set<Id<Person>> ids = new HashSet<>();
		for (Person person : population.getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			if(plan!=null){ //avoid persons without activities
				for (PlanElement pe : plan.getPlanElements()) {
					if (pe instanceof ActivityImpl) {
					ActivityImpl act = (ActivityImpl) pe;
					if ((act.getCoord().getX()<0) || (act.getCoord().getY()<0)) { ids.add(person.getId()); }
					}
				}
			}
		}
		return ids;
	}	

//////////////////////////////////////////////////////////////////////

	public static Set<Id<Person>> identifyPlansWithTooLongWalkTrips(final Population population) {
	Set<Id<Person>> ids = new HashSet<>();
	for (Person person : population.getPersons().values()) {
		Plan plan = person.getSelectedPlan();
		if(plan!=null){ //avoid persons without activities
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Leg) {
					Leg leg = (Leg) pe;
					if ((leg.getMode().equals(MZConstants.WALK))&&(leg.getRoute().getDistance()>10000.0)) {ids.add(person.getId()); }
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
			String hhnr = (String) populationAttributes.getAttribute(person.getId().toString(), MZConstants.HOUSEHOLD_NUMBER);
			CoordImpl homeCoord = (CoordImpl)householdAttributes.getAttribute(hhnr, MZConstants.COORD);

			if(plan!=null){ //avoid persons without activities
				for (int i=0; i<plan.getPlanElements().size(); i=i+2) {
					Activity act = (ActivityImpl)plan.getPlanElements().get(i);
					if ((act.getCoord().getX() == homeCoord.getX()) && (act.getCoord().getY() == homeCoord.getY())) {
						if (!act.getType().equals(MZConstants.HOME)) {
							act.setType(MZConstants.HOME);
							counter++;
	//						System.out.println("        pid=" + p.getId() + "; act_nr=" + (i/2) + ": set type to '"+HOME+"'");
						}
					}else if(act.getType().equals(MZConstants.HOME) & !((act.getCoord().getX() == homeCoord.getX()) && (act.getCoord().getY() == homeCoord.getY()))){
						act.getCoord().setXY(homeCoord.getX(), homeCoord.getY());
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
				CoordImpl workCoord = (CoordImpl)populationAttributes.getAttribute(person.getId().toString(), MZConstants.WORK_LOCATION_COORD);
				
				if(plan!=null){ //avoid persons without activities
					for (int i=0; i<plan.getPlanElements().size(); i=i+2) {
						Activity act = (ActivityImpl)plan.getPlanElements().get(i);
						if ((act.getCoord().getX() == workCoord.getX()) && (act.getCoord().getY() == workCoord.getY())) {
							if (!act.getType().equals(MZConstants.WORK)) {
							act.setType(MZConstants.WORK);
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
	

public static Set<Id<Person>> identifyPlansWithUndefinedNegCoords(final Population population) {
	Set<Id<Person>> ids = new HashSet<>();
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

	public static Set<Id<Person>> identifyPlansWithoutBestPrecision(final Population population) {
	Set<Id<Person>> ids = new HashSet<>();
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
	
	
	
//////////////////////////////////////////////////////////////////////
	/**
	*  
	* This method handles legs that cross the border.
	* 
	* @see  #handleTripGoingOutOfCountry
	* @see  #handleTripEnteringCountry
	*  
	*/	
	
	
	public static void HandleBorderCrossingTrips(final Population population, final ObjectAttributes wegeAttributes, Set<String> border_crossing_wids, String countryCode) {	
		
	final String COUNTRY = countryCode;
	HashMap<Id, ArrayList<Tuple<Integer, PlanElement>>> toAdd =  new HashMap<Id, ArrayList<Tuple<Integer, PlanElement>>>();
	HashMap<Id,ArrayList<PlanElement>> toRemove = 	new HashMap<Id,ArrayList<PlanElement>>();
	Set<Id<Person>> overnight_pids = new HashSet<>();
		
	for(String wid: border_crossing_wids){
		
	
	String sland = (String) wegeAttributes.getAttribute(wid.toString(), MZConstants.START_COUNTRY);
	String zland = (String) wegeAttributes.getAttribute(wid.toString(), MZConstants.END_COUNTRY);

		if(sland.equals(COUNTRY) &&  !zland.equals(COUNTRY)){
			handleTripGoingOutOfCountry(population, wid, COUNTRY, wegeAttributes);
			overnight_pids.add(Id.create(wid.toString().substring(0, wid.toString().indexOf('-')).trim(), Person.class));
		}else if(!sland.equals(COUNTRY) &&  zland.equals(COUNTRY)){
			handleTripEnteringCountry(population, wid, COUNTRY, wegeAttributes, toAdd, toRemove);
			overnight_pids.remove(Id.create(wid.toString().substring(0, wid.toString().indexOf('-')).trim(), Person.class));
		}else{throw new RuntimeException("This should never happen! Wege "+ wid+ " doesn't cross the border");}
		
		
	 }//end loop for all wids
	
	addPlanElements(population,toAdd);
	removePlanElements(population, toRemove);
	removePlanElementsAfterLastBorderCrossing(population, overnight_pids);
	
	}//end method
//////////////////////////////////////////////////////////////////////
/**
*  
* This method goes through the stages of the leg, identifies 
* the border crossing , and modifies the leg start_time so that
* it only considers the data after the border crossing.
* It also eliminates all previous activities and legs that
* are executed outside the country.
*<p> 
*For cases of people that are re-entering the country (went out in the same plan),
*if the border crossings are different, then adds a virtual new leg with mode
*"abroad: teleport" between both border locations/activities
*
*
*
* @param  population the population 
* @param  wid the id of the leg going out of the country
* @param  country the country code  
* @param  wegeAttributes attributes of the leg and it's stages
*/	

	@SuppressWarnings("deprecation")
	private static void handleTripEnteringCountry(Population population, String wid, String country, ObjectAttributes wegeAttributes, HashMap<Id, ArrayList<Tuple<Integer, PlanElement>>> toAdd, HashMap<Id,ArrayList<PlanElement>> toRemove){
	
		Id<Person> pid = Id.create(wid.toString().substring(0, wid.toString().indexOf('-')).trim(), Person.class);
		
		if(pid.toString().equals("87011")){
			System.out.println(pid);
		}
		
		
		int legNumber = Integer.parseInt(wid.toString().substring(wid.toString().indexOf('-')+1));
		//maybe legNumber is not the best way to index, because some planElements are deleted (the ones outside Switzerland)
		// to overcome this issue, all plan elements are stored first in toRemove, and only in the end are deleted.
		ArrayList<PlanElement> elementsToRemove = null;
		
		List<PlanElement> planElements = population.getPersons().get(pid).getSelectedPlan().getPlanElements();
		LegImpl leg = (LegImpl) planElements.get(2*legNumber-1);
		ActivityImpl previousActivity = (ActivityImpl) planElements.get(2*legNumber-2);
		
		boolean immediate_return = false;
		
		String type = previousActivity.getType();
		//if goes in via plane, specify previous activity as airport, otherwise as border
		if(leg.getMode().equals(MZConstants.PLANE)){
			if(!previousActivity.getType().contains(MZConstants.AIRPORT) && !previousActivity.getType().contains(MZConstants.BORDER)){
				previousActivity.setType(MZConstants.AIRPORT.concat(": ").concat(type));
			}else immediate_return = true;
		}else{
			if(!previousActivity.getType().contains(MZConstants.AIRPORT) && !previousActivity.getType().contains(MZConstants.BORDER)){
				previousActivity.setType(MZConstants.BORDER.concat(": ").concat(type));
			}else immediate_return = true;
		}
		
		
		int curr_mode = Integer.MAX_VALUE;
		Coord curr_start_coord = previousActivity.getCoord();
		boolean start = false;
		
		for(int i=1; i<= (Integer) wegeAttributes.getAttribute(wid.toString(), MZConstants.NUMBER_STAGES); i++){
						
			Etappe etappe = (Etappe) wegeAttributes.getAttribute(wid.toString(), MZConstants.STAGE.concat(String.valueOf(i)));
			
			if(!etappe.getStartCountry().equals(country) && etappe.getEndCountry().equals(country)){
				leg.setDepartureTime(etappe.getDepartureTime());
				leg.setTravelTime(etappe.getArrivalTime()-leg.getDepartureTime());
				previousActivity.setEndTime(leg.getDepartureTime());
				curr_start_coord = (etappe.getStartCoord());
				start = true;
			}
			else if(start){
				if(etappe.getModeInteger()<curr_mode){
				curr_mode = etappe.getModeInteger();
				leg.setMode(etappe.getMode());
				}
				leg.setArrivalTime(etappe.getArrivalTime());
				leg.setTravelTime(etappe.getArrivalTime()-leg.getDepartureTime());
				curr_start_coord = (etappe.getStartCoord());
			}
		}	
				
		
		//identify if out and in - border crossings are the same, otherwise it necessary to create new virtual activity. 
		boolean different_border_crossing = false;
		if(immediate_return && !curr_start_coord.equals(previousActivity.getCoord())){
			different_border_crossing = true;				
		}else{
			previousActivity.setCoord(curr_start_coord);
		}
		
	
	//--------------------------------------------------------------------------------------------------------------------------------------
		
			int index = planElements.indexOf(previousActivity);
			String crossing_type = previousActivity.getType().substring(0, previousActivity.getType().indexOf(':')).trim();
							
			if(toRemove.containsKey(pid)){
				elementsToRemove = toRemove.get(pid);
			}
			else{
				elementsToRemove = new ArrayList<PlanElement>(10);
			}
							
						
			if(index>0){
				boolean cont = true;
				if(immediate_return){
				//immediate return, only do something if different border crossing is used
				// if different border crossing is used, a new virtual activity is created with a connecting teleport leg
					if(different_border_crossing){
						LegImpl teleportLeg = new LegImpl(leg);
						teleportLeg.setDepartureTime(previousActivity.getEndTime());
						teleportLeg.setArrivalTime(previousActivity.getEndTime());
						teleportLeg.setTravelTime(0);
						teleportLeg.setMode(MZConstants.ABROAD_TELEPORT);
						GenericRouteImpl route = new GenericRouteImpl(null, null);
						teleportLeg.setRoute(route);
						route.setDistance(0);
						route.setTravelTime(leg.getTravelTime());
						
						if(toAdd.get(pid) == null){ toAdd.put(pid, new ArrayList<Tuple<Integer,PlanElement>>());}	
						toAdd.get(pid).add(new Tuple<Integer, PlanElement>(index+1, teleportLeg));
						
						ActivityImpl virtualActivity = new ActivityImpl(previousActivity);
						virtualActivity.setStartTime(previousActivity.getEndTime());
						virtualActivity.setCoord(curr_start_coord);
						toAdd.get(pid).add(new Tuple<Integer, PlanElement>(index+2, virtualActivity));
												
					}
					
					
				}else{
				//not immediate return, therefore there are activities and legs outside switzerland that need to be eliminated
				//remove previous plan elements (executed outside switzerland) until "airport" or "border" activity is found 
				// Possibilities:	
				//1) all previous activities and legs are outside switzerland -> remove all
				//2) there's a previous "airport" or "border" activity, thus the person left and entered switzerland on the same plan. 
				//	2.1) if the coords of these two match (same airport or border pass) merge both activities and fix start and end times accordingly
				//  2.2) if the coords don't match, create intermediate leg with mode "teleport" to keep consistency on plan.
					
						for (int j=index-1; cont && j>=0 ;j--) {
							
							boolean delete = true;
							PlanElement pe = planElements.get(j);
							if(pe instanceof Activity){
								Activity activity = (Activity) pe;
								if(activity.getType().contains(MZConstants.AIRPORT) || activity.getType().contains(MZConstants.BORDER)){
									
									//going in to switzerland via the same path that went out -> merge activites  (2.1)
									if(activity.getType().contains(crossing_type) && activity.getCoord().equals(previousActivity.getCoord())){ 
									previousActivity.setStartTime(activity.getStartTime());
									cont = false;
									
									}else{
									//going in to switzerland via other path -> create "teleport" leg  (2.2)
									LegImpl teleportLeg = new LegImpl(leg);
									teleportLeg.setDepartureTime(activity.getEndTime());
									teleportLeg.setArrivalTime(previousActivity.getStartTime());
									teleportLeg.setMode(MZConstants.ABROAD_TELEPORT);
									GenericRouteImpl route = new GenericRouteImpl(null, null);
									teleportLeg.setRoute(route);
									route.setDistance(0);
									route.setTravelTime(leg.getTravelTime());
									if(toAdd.get(pid) == null){ toAdd.put(pid, new ArrayList<Tuple<Integer,PlanElement>>());}	
									toAdd.get(pid).add(new Tuple<Integer, PlanElement>(index, teleportLeg));
									delete = false; 
									cont = false;
										
									}
								}
							}
							if(delete){elementsToRemove.add(pe);}
						
						}
					}
				
			}
		
			toRemove.put(pid, elementsToRemove);
	}

//////////////////////////////////////////////////////////////////////
	/**
	 *  
	 * This method goes through the stages of the leg, identifies 
	 * the border crossing , and modifies the leg end_time so that
	 * it only considers the data before the border crossing.
	 * It also adds at the beginning of next activity's type,
	 * either if the person it's at an "aiport" (if leg's mode is plane)
	 * or at a "border" (for any other mode).
	 *
	 * @param  population the population 
	 * @param  wid the id of the leg going out of the country
	 * @param  country the country code  
	 * @param  wegeAttributes attributes of the leg and it's stages
	 */	
	
	
	private static void handleTripGoingOutOfCountry(Population population, String wid, String country, ObjectAttributes wegeAttributes){
	
			Id<Person> pid = Id.create(wid.toString().substring(0, wid.toString().indexOf('-')).trim(), Person.class);
			int legNumber = Integer.parseInt(wid.toString().substring(wid.toString().indexOf('-')+1));
			//maybe legNumber is not the best way to index, because some planElements are deleted (the ones outside Switzerland)
			// to overcome this issue, all plan elements are stored first in toRemove, and only in the end are deleted.
			
			List<PlanElement> planElements = population.getPersons().get(pid).getSelectedPlan().getPlanElements();
			LegImpl leg = (LegImpl) planElements.get(2*legNumber-1);
			ActivityImpl nextActivity = (ActivityImpl) planElements.get(2*legNumber);
			
			boolean cont = true;
			int etappen = 1;
			int curr_mode = Integer.MAX_VALUE;
			String type = nextActivity.getType();
			
			//if goes out via plane, specify next activity as airport, otherwise as border
			if(leg.getMode().equals(MZConstants.PLANE)){
				nextActivity.setType(MZConstants.AIRPORT.concat(": ").concat(type));
			}else {
				nextActivity.setType(MZConstants.BORDER.concat(": ").concat(type));
			}
									
			//modify leg and replace with information from before the border crossing (in MZ2010, a new etappe always start at border crossing!)
						
			while(cont){
				if(etappen > (Integer) wegeAttributes.getAttribute(wid.toString(), MZConstants.NUMBER_STAGES)){
					throw new RuntimeException("This should never happen!  Wege id ("+wid+") doesn't cross border!");
					}
			
				Etappe etappe = (Etappe) wegeAttributes.getAttribute(wid.toString(), MZConstants.STAGE.concat(String.valueOf(etappen)));
				
				if(etappe.getStartCountry().equals(country) && etappe.getEndCountry().equals(country)){
					if(etappe.getModeInteger()<curr_mode){// && (leg.getMode().equals("plane")? !etappe.getMode().equals("plane"):true)){
						curr_mode = etappe.getModeInteger();
						leg.setMode(etappe.getMode());
					}
						leg.setArrivalTime(etappe.getArrivalTime());
						leg.setTravelTime(etappe.getArrivalTime()-leg.getDepartureTime());
						nextActivity.setCoord(etappe.getEndCoord());
						nextActivity.setStartTime(leg.getArrivalTime());
														
					
				}else{ cont = false;}
				
				
				etappen++;
			}
				
		
		
		
	}

//////////////////////////////////////////////////////////////////////
	
	private static void removePlanElementsAfterLastBorderCrossing(Population population, Set<Id<Person>> overnight_pids){
		
		for(Id<Person> id: overnight_pids){
			List<PlanElement> planElements = population.getPersons().get(id).getSelectedPlan().getPlanElements();
			
			boolean cont = true;
			
			for(int i=planElements.size()-1; cont; i--){

				PlanElement pe = planElements.get(i);
				
				if(planElements.get(i) instanceof Activity){
					Activity activity = (Activity) pe;
					if(activity.getType().contains(MZConstants.BORDER) || activity.getType().contains(MZConstants.AIRPORT)){
						cont = false;
					}else{planElements.remove(i);}
					
				}else{planElements.remove(i);}
				
			}
		}
	}
	
//////////////////////////////////////////////////////////////////////
	
	private static void addPlanElements(Population population,  HashMap<Id, ArrayList<Tuple<Integer, PlanElement>>> toAdd){

		for(Id id: toAdd.keySet()){
			
			ArrayList<Tuple<Integer, PlanElement>> elementsToAdd = toAdd.get(id);
			Person person = population.getPersons().get(id);
			
			for(Tuple<Integer, PlanElement> pair : elementsToAdd){
				person.getSelectedPlan().getPlanElements().add(pair.getFirst(), pair.getSecond());
			}

			
			
		
		}
	
	}
	
//////////////////////////////////////////////////////////////////////
	
	private static void removePlanElements(Population population,  HashMap<Id,ArrayList<PlanElement>> toRemove){
	
		for(Id id: toRemove.keySet()){
			
			Person person = population.getPersons().get(id);
			person.getSelectedPlan().getPlanElements().removeAll(toRemove.get(id));
			
		}
		
	}
	
	
//////////////////////////////////////////////////////////////////////

	public static Set<Id<Person>> identifyPlansOutOfSwitzerland(final Population population, final ObjectAttributes wegeAttributes, String countryCode) {
		Set<Id<Person>> ids = new HashSet<>();
		for (Person person : population.getPersons().values()) {	
			Plan plan = person.getSelectedPlan();
			if(plan!=null){ //avoid persons without activities
				boolean out = true;
				int legCounter = 0;
				for (PlanElement pe : plan.getPlanElements()) {
					if (pe instanceof Leg) {
						legCounter++;
						String sland = (String) wegeAttributes.getAttribute(person.getId().toString().concat("-").concat(String.valueOf(legCounter)),MZConstants.START_COUNTRY);
						String zland = (String) wegeAttributes.getAttribute(person.getId().toString().concat("-").concat(String.valueOf(legCounter)),MZConstants.END_COUNTRY);
						if(sland.equals(countryCode) || zland.equals(countryCode)){
							out = false;
							break;
						}
					}
				}
				if(out){
					ids.add(person.getId());
					person.getPlans().clear();
				}
				
			}
		}
		return ids;
	}	


//////////////////////////////////////////////////////////////////////

	public static ArrayList<Set<?>> identifyCrossBorderWeges(final Population population, final ObjectAttributes wegeAttributes, String countryCode) {
		Set<String> wids = new LinkedHashSet<String>();
		Set<Id<Person>> pids = new LinkedHashSet<>();
		
		for (Person person : population.getPersons().values()) {	
			
			Plan plan = person.getSelectedPlan();
			if(plan!=null){ //avoid persons without activities
				int legCounter = 0;
				for (PlanElement pe : plan.getPlanElements()) {
					if (pe instanceof Leg) {
						@SuppressWarnings("unused")
						Leg leg = (Leg) pe;
						legCounter++;
						String wid = person.getId().toString().concat("-").concat(String.valueOf(legCounter));
						String sland = (String) wegeAttributes.getAttribute(wid,MZConstants.START_COUNTRY);
						String zland = (String) wegeAttributes.getAttribute(wid,MZConstants.END_COUNTRY);
						if((!sland.equals(countryCode) ^ !zland.equals(countryCode))){
						 wids.add(wid);
						 pids.add(person.getId());
							
					}
				}
			}
			
			
			}
		}
		ArrayList<Set<?>> ids = new ArrayList<Set<?>>();
		ids.add(pids);
		ids.add(wids);
		return ids;
	}	

//////////////////////////////////////////////////////////////////////

	public static Set<Id> removeWegesOutsideSwitzerland(final Population population, final Set<Id> ids, ObjectAttributes wegeAttributes) {
		
		Set<PlanElement> elements = new LinkedHashSet<PlanElement>();
		
		for (Id id : ids) {
			int legCounter = 0;
			Person person = population.getPersons().get(id);
			Plan plan = person.getSelectedPlan();
			for (int i=0; i<plan.getPlanElements().size();i++) {
				
					PlanElement pe = plan.getPlanElements().get(i);
					if (pe instanceof Leg) {
						legCounter++;
						String wid = person.getId().toString().concat("-").concat(String.valueOf(legCounter));
						
						Leg leg = (Leg) pe;
						Activity act = (Activity) plan.getPlanElements().get(i+1);
						
						String sland = (String) wegeAttributes.getAttribute(wid,MZConstants.START_COUNTRY);
						String zland = (String) wegeAttributes.getAttribute(wid,MZConstants.END_COUNTRY);
						if((!sland.equals(MZConstants.SWISS_CODE) && !zland.equals(MZConstants.SWISS_CODE))){
						elements.add(leg);
						if(act.getType().contains(MZConstants.AIRPORT)){
							for(int j=i-1;j==0;j-=2){
								PlanElement pel = plan.getPlanElements().get(j);
							if(pel instanceof Activity){
								Activity activity = (Activity) pel;
								if(activity.getType().contains(MZConstants.AIRPORT)){activity.setEndTime(act.getEndTime());}
							}
							}
						}
						elements.add(act);
						}
						
					}
			}
			
			plan.getPlanElements().removeAll(elements);
		}
		return ids;
	}	
	
//////////////////////////////////////////////////////////////////////

	public static final Set<Id> identifyPlansWithRoundTrips(final Population plans) {
		Set<Id> ids = new HashSet<Id>();
		for (Person p : plans.getPersons().values()) {
			Plan plan = p.getSelectedPlan();
			Activity prevAct = null;
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					Activity act = (Activity) pe;
					if (prevAct != null) {
						Coord prevc = prevAct.getCoord();
						Coord currc = act.getCoord();
						if ((currc.getX()==prevc.getX())&& (currc.getY()==currc.getY())) { ids.add(p.getId()); }
					}
					prevAct = act;
				}
			}
		}
		return ids;
	}	
	
	
//////////////////////////////////////////////////////////////////////

	public static void classifyActivityChains(Population population){
		
		HashMap<String,Integer> actChains = new HashMap<String,Integer>();
			
		for(Person person: population.getPersons().values()){
		
			StringBuilder actChain = new StringBuilder();
			Plan plan = person.getSelectedPlan();
			
			if(plan!=null){
				for(PlanElement pe: plan.getPlanElements()){
					
					if(pe instanceof Activity){
						
						ActivityImpl act = (ActivityImpl) pe;
						String type = act.getType();
						
						if(type.contains(MZConstants.WORK)
								||type.contains(MZConstants.BUSINESS)
								||type.contains(MZConstants.DIENSTFAHRT)){actChain.append("W");}
						
						else if(type.contains(MZConstants.BUSINESS)
								||type.contains(MZConstants.DIENSTFAHRT)) {actChain.append("B");}
						
						else if(type.contains(MZConstants.LEISURE)
								||type.contains(MZConstants.ACCOMPANYING_CHILDREN)
								|| type.contains(MZConstants.ACCOMPANYING_NOT_CHILDREN)
								|| type.contains(MZConstants.ERRANDS)
								|| type.contains(MZConstants.OTHER)
								|| type.contains(MZConstants.FOREIGN_PROPERTY)
								|| type.contains(MZConstants.OVERNIGHT)
								|| type.contains(MZConstants.PSEUDOETAPPE)) {actChain.append("L");}
						
						else if(type.contains(MZConstants.ACCOMPANYING_CHILDREN)
								|| type.contains(MZConstants.ACCOMPANYING_NOT_CHILDREN)
								|| type.contains(MZConstants.ACCOMPANYING)
								|| type.contains(MZConstants.ERRANDS)
								|| type.contains(MZConstants.OTHER)
								|| type.contains(MZConstants.FOREIGN_PROPERTY)
								|| type.contains(MZConstants.OVERNIGHT)
								|| type.contains(MZConstants.PSEUDOETAPPE)	) {actChain.append("O");}
						
						else if(type.contains(MZConstants.SHOPPING)) {actChain.append("S");}
						
						else if(type.contains(MZConstants.EDUCATION)) {actChain.append("E");}
						
						else if(type.contains(MZConstants.HOME)) {actChain.append("H");}
						
						else{throw new RuntimeException("This should never happen! Type: " + type + " doesn't exist " + person.getId());}
					}
					
					
				}
				
				if(actChains.containsKey(actChain.toString())){
					int total = actChains.get(actChain.toString());
					actChains.put(actChain.toString(), total+1);
				}else{
					actChains.put(actChain.toString(),1);	
				}
				
				
			}		
		
		}
		MyCollectionUtils.printMap(actChains);
		System.out.println("Total of different activity chains: " + actChains.keySet().size());
		
		
	}
	
//////////////////////////////////////////////////////////////////////	
	
	public static void changeToMatsimModes(Population population) {
		Set<String> unknownModes = new HashSet<String>();

		for (Person person : population.getPersons().values()) {

			Plan plan = person.getSelectedPlan();

			if(plan!=null){
				for(PlanElement pe: plan.getPlanElements()){

					if(pe instanceof Leg){
						Leg leg = (Leg) pe;
						
						String mode = leg.getMode();
						if (mode.equals(MZConstants.PLANE)
								|| mode.equals(MZConstants.TRAIN)
								|| mode.equals(MZConstants.SHIP)
								|| mode.equals(MZConstants.TAXI)
								|| mode.equals(MZConstants.TRAM)
								|| mode.equals(MZConstants.BUS)
								|| mode.equals(MZConstants.SONSTINGER_OEV)
								|| mode.equals(MZConstants.POSTAUTO)
								|| mode.equals(MZConstants.REISECAR)
								|| mode.equals(MZConstants.TRAIN)) {
							leg.setMode(TransportMode.pt);   //PUBLIC TRANSPORT
						} else if (mode.equals(MZConstants.WALK)) {
							leg.setMode(TransportMode.walk);  //WALK

						} else if (mode.equals(MZConstants.BICYCLE)
								|| mode.equals(MZConstants.SKATEBOARD)
								|| mode.equals(MZConstants.MOFA)) {
							leg.setMode(TransportMode.bike);  //BICYCLE

						} else if(mode.equals(MZConstants.CAR)
								|| mode.equals(MZConstants.MOTORCYCLE)
								|| mode.equals(MZConstants.TRUCK)
								){
							leg.setMode(TransportMode.car);  //CAR

						} else {
							unknownModes.add(mode);
						}
					}
				}
			}
		}

		if (!unknownModes.isEmpty()) {
			log.warn("Unhandled modes: "
					+ unknownModes.toString());
		}
	}



	public static void removePersonsWithoutPlan(Population population) {
		final Map<Id<Person>, ? extends Person> persons = population.getPersons();
		ArrayList<Id<Person>> toRemove = new ArrayList<>(persons.size());
		
		for (Id<Person> id : persons.keySet()) {
			Person person = persons.get(id);
			Plan plan = person.getSelectedPlan();

			if(plan == null)
				toRemove.add(id);
		}

		for (Id<Person> id : toRemove)
			persons.remove(id);
		
		log.info("Removed " + toRemove.size() + " persons without plan.");
	}



	public static void analyzeActivityTypesAndLengths(Population population) throws ExecutionException {
		LoadingCache<String, SummaryStatistics> activityDuration =
				CacheBuilder.newBuilder().build(CacheLoader.from(
						new Supplier<SummaryStatistics>() {
								@Override
								public SummaryStatistics get() {
									return new SummaryStatistics(); }
				}));
		
		for (Person p : population.getPersons().values()) {
			if (p.getPlans().size() == 0)
				continue;
				
			Plan plan = p.getPlans().get(0);
			
			List<PlanElement> planElements = plan.getPlanElements();
			for (PlanElement pe : planElements) {
				if (!(pe instanceof Activity))
					continue;
				Activity activity = (Activity)pe;
				
				double startTime = activity.getStartTime();
				double endTime = activity.getEndTime();
				
				SummaryStatistics typeStats = activityDuration.get(activity.getType());
				if (endTime != Time.UNDEFINED_TIME) {
					if (startTime == Time.UNDEFINED_TIME)
						startTime = 0;
					typeStats.addValue(endTime - startTime);
				}
			}
		}
		
		ConcurrentMap<String, SummaryStatistics> activityDurationMap = activityDuration.asMap();
		{
			int i = 0;
			final StringBuffer s = new StringBuffer();
			for (final String actType : activityDurationMap.keySet()) {
				final SummaryStatistics stats = activityDurationMap.get(actType);

				s.append(String.format("<param name=\"activityType_%d\" value=\"%s\" />\n", i, actType));
				s.append(String.format("<param name=\"activityPriority_%d\" value=\"1\" />\n", i));
				s.append(String.format("<param name=\"activityTypicalDuration_%d\" value=\"%s\" />\n", i, Time.writeTime(stats.getMean())));
				s.append("\n");
				i++;
			}
			log.info("All activities:\n" + s.toString());
		}
	}



	public static void setMZ2000LegModesWithEtappenInfo(
			Population population,
			TreeMap<String, ArrayList<playground.acmarmol.matsim2030.forecasts.timeSeriesUpdate.loaders.etappes.Etappe>> etappes) {
		
		
		for(Person person: population.getPersons().values()){
			
			Id id  = person.getId();
			
			Plan plan = person.getSelectedPlan();
			
			if(plan==null)
				continue;
			
			ArrayList<playground.acmarmol.matsim2030.forecasts.timeSeriesUpdate.loaders.etappes.Etappe> etappen = etappes.get(id.toString());
			int legCounter=0;
			for(PlanElement pe : plan.getPlanElements()){
				
				if(!(pe instanceof Leg))
					continue;
				
				legCounter++;
				LegImpl leg = (LegImpl) pe;
				int curr_mode = Integer.MAX_VALUE;
				String mode = null;
				
				for(playground.acmarmol.matsim2030.forecasts.timeSeriesUpdate.loaders.etappes.Etappe etappe: etappen){
					
					if(etappe.getWegeNr()>legCounter)
						break;
					
					if(etappe.getModeInteger()<curr_mode){
						curr_mode = etappe.getModeInteger();
						mode = etappe.getWegeEquivalentModeFromEtappeMode();
						
						}
					
				}
				
				leg.setMode(mode);			
			}
		}
	}
	
//////////////////////////////////////////////////////////////////////
	
	public static void setHomeLocationsMZ2000FromHouseholdAdress(final Population population,
			 final ObjectAttributes populationAttributes,
			 final ObjectAttributes householdAttributes,
			 final ObjectAttributes wegeAttributes) {
		
		HashMap<Id, ArrayList<Tuple<Integer, Integer>>> homes_info = new HashMap<Id,ArrayList<Tuple<Integer, Integer>>>();
		int counter = 0;
		

			for (Person person : population.getPersons().values()) {	
				String hhnr = (String) populationAttributes.getAttribute(person.getId().toString(), MZConstants.HOUSEHOLD_NUMBER);
				String hh_address = (String) householdAttributes.getAttribute(hhnr, MZConstants.ADDRESS);
				
				Plan plan = person.getSelectedPlan();
				
				if(plan==null) //avoid persons without activities
					continue;
				
					int legCounter = 0;
					for (PlanElement pe : plan.getPlanElements()) {
						if (pe instanceof Leg) {
							
							legCounter++;
							String address_start = (String) wegeAttributes.getAttribute(person.getId().toString().concat("-").concat(String.valueOf(legCounter)),MZConstants.ADDRESS_START);
							String adress_end = (String) wegeAttributes.getAttribute(person.getId().toString().concat("-").concat(String.valueOf(legCounter)),MZConstants.ADDRESS_END);
							
							if(!(address_start.equals(hh_address) || adress_end.equals(hh_address)))
								continue;
							
							int pos=0;;
							if(address_start.equals(hh_address))
								pos=-1;
							counter++;
							if(homes_info.get(person.getId())==null)
								homes_info.put(person.getId(), new ArrayList<Tuple<Integer, Integer>>());
							homes_info.get(person.getId()).add(new Tuple<Integer, Integer>(legCounter, pos));
						}
					}
					
					
				
			}
			
		setHomeLocations(population, homes_info);
			
		System.out.println("      Number of activities set to work: " + counter);
	}



	private static void setHomeLocations(
			Population population,
			HashMap<Id, ArrayList<Tuple<Integer, Integer>>> homes_info) {
		
		   Iterator it = homes_info.entrySet().iterator();
		    while (it.hasNext()) {
		        Map.Entry pairs = (Map.Entry)it.next();
		        Id id = (Id) pairs.getKey();
		        ArrayList<Tuple<Integer, Integer>> legs = (ArrayList<Tuple<Integer, Integer>>) pairs.getValue();
		            
		       	        
		       Person person = population.getPersons().get(id);
		       List<PlanElement> pes = person.getSelectedPlan().getPlanElements();
		       PlanElement[] pesArray = pes.toArray(new PlanElement[pes.size()]);
		       
		       for(Tuple<Integer,Integer> leg:legs){
		    	   
		    	   ActivityImpl activity = (ActivityImpl) pesArray[2*(leg.getFirst()+leg.getSecond())]; 
		    	   activity.setType(MZConstants.HOME); 
		       }
		        
		       it.remove(); // avoids a ConcurrentModificationException
		    }
		
		
	}



	public static void setHomeLocationsMZ2000AccordingToActivitySequence(
			Population population) {
		
		for(Person person: population.getPersons().values()){
			
			Plan plan = person.getSelectedPlan();
			
			if(plan==null)
				continue;
			
			String lastActivityType = null;
			
			for(PlanElement pe: plan.getPlanElements()){
				
				if (pe instanceof ActivityImpl) {
					ActivityImpl act = (ActivityImpl) pe;
					if(act.getType().equals(lastActivityType))
						act.setType(MZConstants.HOME);
						
					lastActivityType = act.getType();					
				}
				
				
			}
			
			
		}
		
		
		
	}


	

	public static void eliminateToursWithUndefinedWegeTimesMZ1989(
			Population population) {
		//removes only home-to-home tours
		
		for(Person person: population.getPersons().values()){
			
			if(person.getId().toString().equals("100071")){
				System.out.println();
			}
			
 			Plan plan = person.getSelectedPlan();
			
			if(plan==null)
				continue;
			
			boolean erase =false;
			ArrayList<PlanElement> toRemove = new  ArrayList<PlanElement>();
			ArrayList<PlanElement> tour = new  ArrayList<PlanElement>();
			
			
			for(PlanElement pe: plan.getPlanElements()){
				System.out.println(pe);
				tour.add(pe);
				if(pe instanceof ActivityImpl){
					ActivityImpl act = (ActivityImpl) pe;
					if(act.getType().equals(MZConstants.HOME)){
						if(erase){
							
							
							toRemove.addAll(tour);
							erase= false;
						}
							
						tour.clear();
					}
						
				}
				
				if(!(pe instanceof Leg))
					continue;
				
				Leg leg = (LegImpl) pe;
				if(leg.getDepartureTime()< 0 || leg.getTravelTime()<0)
					erase=true;			
				
			
			}
			if(erase)
				toRemove.addAll(tour);
		
		plan.getPlanElements().removeAll(toRemove);	
			
		}
		
		
	}

//////////////////////////////////////////////////////////////////////	
	
	public static void recodeActivityTypesHWELS(Population population){
		
		for(Person person: population.getPersons().values()){
		
			Plan plan = person.getSelectedPlan();
			
			if(plan!=null){
				for(PlanElement pe: plan.getPlanElements()){
					
					if(pe instanceof Activity){
						
						ActivityImpl act = (ActivityImpl) pe;
						String type = act.getType();
						
						if(type.contains(MZConstants.WORK)
								||type.contains(MZConstants.BUSINESS)
								||type.contains(MZConstants.DIENSTFAHRT))
						{act.setType(MZConstants.WORK);}
						
						else if(type.contains(MZConstants.LEISURE)
								||type.contains(MZConstants.ACCOMPANYING_CHILDREN)
								|| type.contains(MZConstants.ACCOMPANYING_NOT_CHILDREN)
								|| type.contains(MZConstants.ERRANDS)
								|| type.contains(MZConstants.OTHER)
								|| type.contains(MZConstants.FOREIGN_PROPERTY)
								|| type.contains(MZConstants.ACCOMPANYING)
								|| type.contains(MZConstants.OVERNIGHT)
								|| type.contains(MZConstants.PSEUDOETAPPE))
						{act.setType(MZConstants.LEISURE);}
						
						//////////////////////////////////////////////////////////////////////
						// code added by staheale
						
						else if(type.contains(MZConstants.BORDER.concat(": ").concat(MZConstants.SHOPPING))){
							act.setType(MZConstants.SHOPPING);
						}
						
						else if(type.contains(MZConstants.BORDER.concat(": ").concat(MZConstants.HOME))){
							act.setType(MZConstants.HOME);
						}
						
						else if(type.contains(MZConstants.BORDER.concat(": ").concat(MZConstants.EDUCATION))){
							act.setType(MZConstants.EDUCATION);
						}
						
						else if(type.contains(MZConstants.AIRPORT.concat(": ").concat(MZConstants.SHOPPING))){
							act.setType(MZConstants.SHOPPING);
						}
						
						else if(type.contains(MZConstants.AIRPORT.concat(": ").concat(MZConstants.HOME))){
							act.setType(MZConstants.HOME);
						}
						
						//////////////////////////////////////////////////////////////////////
						
//						else if(type.contains(MZConstants.SHOPPING))
//						{act.setType(MZConstants.SHOPPING);}
//						
//						else if(type.contains(MZConstants.EDUCATION))
//						{act.setType(MZConstants.EDUCATION);}
//						
//						else if(type.contains(MZConstants.HOME))
//						{act.setType(MZConstants.HOME);}
						
					}
					
					
				}
				
			}
		
		}
	}
	//////////////////////////////////////////////////////////////////////

	@SuppressWarnings("deprecation")
	public static void removeDuplicateActivities(Population population) {
		for (Person person : population.getPersons().values()) {

			Plan plan = person.getSelectedPlan();

			if (plan == null)
				continue;
			
			Stack<PlanElement> newPlanElements = new Stack<PlanElement>();
			
			ActivityImpl prevAct = null;
			
			List<PlanElement> planElements = plan.getPlanElements();
			for (PlanElement pe : planElements) {
				
				PlanElement newPe = pe;

				if (pe instanceof Activity) {
					
					ActivityImpl act = (ActivityImpl) pe;
					
					if (prevAct != null) {
						String type = act.getType();
						
						if (type.equals(prevAct.getType())) {
							PlanElement leg = newPlanElements.pop();
							assert(leg instanceof Leg);
							PlanElement prevActPopped = newPlanElements.pop();
							assert(prevActPopped == prevAct);
							
							// Choose activity with longest duration
							if (act.getEndTime() - act.getStartTime() > prevAct.getEndTime() - prevAct.getStartTime())
								act.setStartTime(prevAct.getStartTime());
							else {
								prevAct.setEndTime(act.getEndTime());
								act = prevAct;
							}
							
							newPe = act;
						}
					}
					
					prevAct = act;
				}
				
				newPlanElements.add(newPe);
			}
			
			planElements.clear();
			planElements.addAll(newPlanElements);

		}
	}
}
