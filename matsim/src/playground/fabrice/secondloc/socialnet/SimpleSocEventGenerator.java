/* *********************************************************************** *
 * project: org.matsim.*
 * SimpleSocEventGenerator.java
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

package playground.fabrice.secondloc.socialnet;

import java.util.Collection;
import java.util.HashMap;

import org.matsim.plans.Person;
import org.matsim.plans.Plans;

import playground.fabrice.secondloc.CoolPlace;

public class SimpleSocEventGenerator implements SocialEventGeneratorI {

	String activityType;
	
	public SimpleSocEventGenerator( String activityType ){
		this.activityType = activityType;
	}
	
	public Collection<SocialEvent> generate( Plans plans ) {
		// This generator generates SocialEvents *only*
		// based on the Knowledge of individuals
		// We assume agents perform a single activity of a given type
		// at a known place 
		// This is called typically in initial demand modelling
		
		HashMap<CoolPlace, SocialEvent> events = new 	HashMap<CoolPlace, SocialEvent>();
		
		for( Person person : plans.getPersons().values() ){
			CoolPlace place = person.getKnowledge().map.getRandomCoolPlace(activityType);
			if( place == null )
				continue;
			SocialEvent event = events.get(place);
			if( event == null ){
				event = new SocialEvent( place );
				events.put( place,	event );
			}
			event.addAttendee(person);
		}
		
		return events.values();
	}
}
