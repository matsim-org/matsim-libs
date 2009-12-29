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
import org.matsim.facilities.Facility;
import org.matsim.population.Act;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;

public class SpatialSocialActTracker implements SocialActGeneratorI {

	public SpatialSocialActTracker(){

	}

	// For each ACT in a PLAN we need to hang other attributes on it, like
	// WHO was there and WHEN and WHERE it was.
	// This object is called a socializing opportunity
	// Associate the activity (-->facility) with the 
	//  socializing opportunity.
	// Agents need to be able to find the socializing opportunity associated
	//  with each of their Acts: person.map.dates gives the socializingopp's;

	public Collection<SocialAct> generateValues(Population plans) {
		HashMap<Activity, SocialAct> events = new HashMap<Activity, SocialAct>();
		SocialAct event = null;

		for( Person person : plans.getPersons().values() ){
//			System.out.println("SSOgen Person "+person.getId()+" ");

			person.getKnowledge().getMentalMap().clearDates();

			Plan plan = person.getSelectedPlan();
			ActIterator it = plan.getIteratorAct();
			while( it.hasNext() ){

				Act act = (Act) it.next();
//				Activity myActivity = person.getKnowledge().getMentalMap().getActivity(act);
				Activity myActivity = act.getFacility().getActivity(act.getType());
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

	public HashMap<Activity, SocialAct> generateMap(Population plans) {
		HashMap<Activity, SocialAct> events = new HashMap<Activity, SocialAct>();
		SocialAct event = null;

		for( Person person : plans.getPersons().values() ){
//			System.out.println("SSOgen Person "+person.getId()+" ");

			person.getKnowledge().getMentalMap().clearDates();

			Plan plan = person.getSelectedPlan();
			ActIterator it = plan.getIteratorAct();
			while( it.hasNext() ){

				Act act = (Act) it.next();
//				Activity myActivity = person.getKnowledge().getMentalMap().getActivity(act);
				//Above lines call code that results in a null pointer. Test
				// michi's new change. Note the Act.setFacility() might not
				// always be kept up-to-date by socialNetowrk code, check this. JH 02-07-2008
				Activity myActivity = act.getFacility().getActivity(act.getType());

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
