/* *********************************************************************** *
 * project: org.matsim.*
 * JavaDEQSim.java
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

package playground.wrashid.deqsim;

import java.util.ArrayList;

import org.matsim.events.Events;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;

public class JavaDEQSim {

	final Plans population;
	
	public JavaDEQSim(final NetworkLayer network, final Plans population, final Events events) {
		// constructor
		this.population = population;
	}
	
	public void run() {
		// do something
		
		/* Das folgende ist einfach ein Beispiel, wie auf die Population zugegriffen 
		 * werden kann und wie man herauskriegt, welche Route die Agenten abfahren
		 * sollen. Dieser Code wird bestimmt nicht hier so stehen bleiben, sondern
		 * spaeter in anderen Klassen verwendet werden.
		 */
		
		for (Person person : this.population.getPersons().values()) {
			Plan plan = person.getSelectedPlan(); // that's the plan the person will execute
			ArrayList<Object> actsLegs = plan.getActsLegs();
			for (int i = 0; i < actsLegs.size(); i++) {
				if (i % 0 == 0) {
					Act act = (Act)actsLegs.get(i);
					// the activity the agent performs
					double departureTime = act.getEndTime(); // the time the agent departs at this activity
				} else {
					Leg leg = (Leg)actsLegs.get(i);
					// the leg the agent performs
					if ("car".equals(leg.getMode())) { // we only simulate car traffic
						Link[] route = leg.getRoute().getLinkRoute(); // these are the links the agent will drive along one after the other.
					}
				}
			}
		}
	}
}
