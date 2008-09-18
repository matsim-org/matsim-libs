/* *********************************************************************** *
 * project: org.matsim.*
 * PlansDefineKnowledge.java
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

package org.matsim.population.algorithms;

import java.util.ArrayList;
import java.util.Iterator;

import org.matsim.facilities.Activity;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.gbl.MatsimRandom;
import org.matsim.population.Knowledge;
import org.matsim.population.Person;
import org.matsim.population.Population;

public class PlansDefineKnowledge {

	private final Facilities facilities;
	
	public PlansDefineKnowledge(final Facilities facilities) {
		this.facilities = facilities;
	}

	public void run(Population plans) {
		System.out.println("    running " + this.getClass().getName() + " algorithm...");

		// get home, work and other activities
		ArrayList<Activity> home_acts = new ArrayList<Activity>();
		ArrayList<Activity> work_acts = new ArrayList<Activity>();
		ArrayList<Activity> other_acts = new ArrayList<Activity>();
		for (Facility f : this.facilities.getFacilities().values()) {
			Iterator<Activity> a_it = f.getActivities().values().iterator();
			while (a_it.hasNext()) {
				Activity a = a_it.next();
				if (a.getType().equals("home")) { home_acts.add(a); }
				else if (a.getType().equals("work")) { work_acts.add(a); }
				else { other_acts.add(a); }
			}
		}

		// set exactly one home and four other activities for each person
		Iterator<Person> p_it = plans.getPersons().values().iterator();
		while (p_it.hasNext()) {
			Person p = p_it.next();
			Knowledge k = p.createKnowledge("created by " + this.getClass().getName());
			int index = MatsimRandom.random.nextInt(home_acts.size());
			k.addActivity(home_acts.get(index),true);
			index = MatsimRandom.random.nextInt(work_acts.size());
			k.addActivity(work_acts.get(index),true);
			for (int i=0; i<4; i++) {
				index = MatsimRandom.random.nextInt(other_acts.size());
				k.addActivity(other_acts.get(index),false);
			}
		}

		System.out.println("    done.");
	}
}
