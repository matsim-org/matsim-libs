/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialSocialOpportunityTracker.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.socialnetworks.interactions;
/**
 * @author jhackney
 */
import java.util.Collection;
import java.util.HashMap;

import org.matsim.basic.v01.BasicPlanImpl.ActIterator;
import org.matsim.facilities.Activity;
import org.matsim.plans.Act;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;

public class SpatialSocialOpportunityTracker implements SocialActGeneratorI {

	public SpatialSocialOpportunityTracker(){

	}

	// For each ACT in a PLAN we need to hang other attributes on it, like
	// WHO was there and WHEN and WHERE it was.
	// This object is called a socializing opportunity
	// Associate the activity (-->facility) with the 
	//  socializing opportunity.
	// Agents need to be able to find the socializing opportunity associated
	//  with each of their Acts: person.map.dates gives the socializingopp's;

	public Collection<SocialAct> generateValues(Plans plans) {
		HashMap<Activity, SocialAct> events = new HashMap<Activity, SocialAct>();
		SocialAct event = null;

		for( Person person : plans.getPersons().values() ){
//			System.out.println("SSOgen Person "+person.getId()+" ");
			
			person.getKnowledge().getMentalMap().clearDates();

			Plan plan = person.getSelectedPlan();
			ActIterator it = plan.getIteratorAct();
			while( it.hasNext() ){

				Act act = (Act) it.next();
				Activity myActivity = person.getKnowledge().getMentalMap().getActivity(act);

				if( myActivity == null ){
					System.out.println(" Act "+act.getLinkId()+" "+act.getType()+" no activity");
					continue;}
				event = events.get(myActivity);
				if( event == null ){
					event = new SocialAct( myActivity );
					events.put( myActivity,	event );
				}
				event.addAttendee(person);
			}
			person.getKnowledge().getMentalMap().addDate(event);
		}
		return events.values();
	}
	
	public HashMap<Activity, SocialAct> generateMap(Plans plans) {
		HashMap<Activity, SocialAct> events = new HashMap<Activity, SocialAct>();
		SocialAct event = null;

		for( Person person : plans.getPersons().values() ){
//			System.out.println("SSOgen Person "+person.getId()+" ");
			
			person.getKnowledge().getMentalMap().clearDates();

			Plan plan = person.getSelectedPlan();
			ActIterator it = plan.getIteratorAct();
			while( it.hasNext() ){

				Act act = (Act) it.next();
				Activity myActivity = person.getKnowledge().getMentalMap().getActivity(act);

				if( myActivity == null ){
					System.out.println(" Act "+act.getLinkId()+" "+act.getType()+" no activity");
					continue;}
				event = events.get(myActivity);
				if( event == null ){
					event = new SocialAct( myActivity );
					events.put( myActivity,	event );
				}
				event.addAttendee(person);
			}
			person.getKnowledge().getMentalMap().addDate(event);
		}
		return events;
	}
}
