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

package org.matsim.plans.algorithms;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import org.matsim.facilities.Activity;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Knowledge;
import org.matsim.plans.Person;
import org.matsim.plans.Plans;
import org.matsim.world.Location;

public class PlansDefineKnowledge extends PlansAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final Random rand = new Random(101);

	//////////////////////////////////////////////////////////////////////
	// constructor
	//////////////////////////////////////////////////////////////////////

	public PlansDefineKnowledge() {
		rand.nextInt();
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	public void run(Plans plans) {
		System.out.println("    running " + this.getClass().getName() + " algorithm...");

		// get home and other activities
		ArrayList<Activity> home_acts = new ArrayList<Activity>();
		ArrayList<Activity> other_acts = new ArrayList<Activity>();
		Iterator<Location> loc_it = Gbl.getWorld().getLayer(Facilities.LAYER_TYPE).getLocations().values().iterator();
		while (loc_it.hasNext()) {
			Facility f = (Facility)loc_it.next();
			Iterator<Activity> a_it = f.getActivities().values().iterator();
			while (a_it.hasNext()) {
				Activity a = a_it.next();
				if (a.getType().equals("home")) { home_acts.add(a); }
				else { other_acts.add(a); }
			}
		}

		// set exactly one home and four other activities for each person
		Iterator<Person> p_it = plans.getPersons().values().iterator();
		while (p_it.hasNext()) {
			Person p = p_it.next();
			Knowledge k = p.createKnowledge("created by " + this.getClass().getName());
			int index = this.rand.nextInt(home_acts.size());
			k.addActivity(home_acts.get(index));
			for (int i=0; i<4; i++) {
				index = this.rand.nextInt(other_acts.size());
				k.addActivity(other_acts.get(index));
			}
		}
		
		System.out.println("    done.");
	}
}
