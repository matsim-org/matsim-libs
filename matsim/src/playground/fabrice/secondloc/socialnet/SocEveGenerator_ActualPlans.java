/* *********************************************************************** *
 * project: org.matsim.*
 * SocEveGenerator_ActualPlans.java
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

import org.matsim.basic.v01.BasicPlan.ActIterator;
import org.matsim.network.Link;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;

import playground.fabrice.secondloc.CoolPlace;

public class SocEveGenerator_ActualPlans implements SocialEventGeneratorI {

	HashMap<Link,CoolPlace> link2cool;

	public SocEveGenerator_ActualPlans( HashMap<Link,CoolPlace> link2cool ){

		this.link2cool = link2cool;

	}

	public Collection<SocialEvent> generate(Plans plans) {
		// This generator generates SocialEvents *only*
		// based on the actual Plans of individuals
		// This is called typically in replanning

		HashMap<CoolPlace, SocialEvent> events = new 	HashMap<CoolPlace, SocialEvent>();

		for( Person person : plans.getPersons().values() ){
			Plan plan = person.getSelectedPlan();
			ActIterator it = plan.getIteratorAct();
			while( it.hasNext() ){

				Link link = (Link) it.next().getLink();
				CoolPlace place = this.link2cool.get( link );

				if( place == null )
					continue;
				SocialEvent event = events.get(place);
				if( event == null ){
					event = new SocialEvent( place );
					events.put( place,	event );
				}
				event.addAttendee(person);

			}
		}
		return events.values();
	}
}
