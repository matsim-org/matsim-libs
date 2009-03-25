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

package playground.marek.deqsim;

import java.util.List;

import org.matsim.api.basic.v01.population.BasicLeg;
import org.matsim.api.basic.v01.population.BasicPlanElement;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.CarRoute;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.events.Events;
import org.matsim.network.NetworkLayer;

public class JavaDEQSim {

	final Population population;
	
	public JavaDEQSim(final NetworkLayer network, final Population population, final Events events) {
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
			List<? extends BasicPlanElement> actsLegs = plan.getPlanElements();
			for (BasicPlanElement pe : actsLegs) {
				if (pe instanceof Activity) {
					Activity act = (Activity) pe;
					// the activity the agent performs
					double departureTime = act.getEndTime(); // the time the agent departs at this activity
				} else if (pe instanceof Leg) {
					Leg leg = (Leg) pe;
					// the leg the agent performs
					if (BasicLeg.Mode.car.equals(leg.getMode())) { // we only simulate car traffic
						List<Link> route = ((CarRoute) leg.getRoute()).getLinks(); // these are the links the agent will drive along one after the other.
					}
				}
			}
		}
	}
}
