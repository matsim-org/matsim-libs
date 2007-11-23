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

package playground.jhackney.interactions;
/**
 * @author jhackney
 */
import java.util.Collection;
import java.util.HashMap;

import org.matsim.basic.v01.BasicPlan.ActIterator;
import org.matsim.facilities.Activity;
import org.matsim.plans.Act;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;

public class SpatialSocialOpportunityTracker implements SocializingOpportunityGeneratorI {

    public SpatialSocialOpportunityTracker(){

    }

    // For each ACT in a PLAN we need to hang other attributes on it, like
    // WHO was there and WHEN and WHERE it was.
    // This object is called a socializing opportunity
    // Associate the activity (-->facility) with the 
    // socializing opportunity
    
    public Collection<SocializingOpportunity>generate(Plans plans) {
	HashMap<Activity, SocializingOpportunity> events = new HashMap<Activity, SocializingOpportunity>();
	for( Person person : plans.getPersons().values() ){
	    Plan plan = person.getSelectedPlan();
	    ActIterator it = plan.getIteratorAct();
	    while( it.hasNext() ){

		Act act = (Act) it.next();
		Activity myActivity = person.getKnowledge().map.getActivity(act);

		if( myActivity == null ){
//		    System.out.println(" Act"+act.getLinkId()+" "+act.getType()+" no activity");
		    continue;}
		SocializingOpportunity event = events.get(myActivity);
		if( event == null ){
		    event = new SocializingOpportunity( myActivity );
		    events.put( myActivity,	event );
		}
		event.addAttendee(person);

	    }
	}	
	return events.values();
    }
}
