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

import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.knowledges.Knowledge;
import org.matsim.knowledges.Knowledges;

public class PlansDefineKnowledge {

	private final ActivityFacilitiesImpl facilities;
	private Knowledges knowledges;
	
	public PlansDefineKnowledge(final ActivityFacilitiesImpl facilities, Knowledges knowledges) {
		this.facilities = facilities;
		this.knowledges = knowledges;
	}

	public void run(PopulationImpl plans) {
		System.out.println("    running " + this.getClass().getName() + " algorithm...");

		// get home, work and other activities
		ArrayList<ActivityOption> home_acts = new ArrayList<ActivityOption>();
		ArrayList<ActivityOption> work_acts = new ArrayList<ActivityOption>();
		ArrayList<ActivityOption> other_acts = new ArrayList<ActivityOption>();
		for (ActivityFacilityImpl f : this.facilities.getFacilities().values()) {
			Iterator<ActivityOption> a_it = f.getActivityOptions().values().iterator();
			while (a_it.hasNext()) {
				ActivityOption a = a_it.next();
				if (a.getType().equals("home")) { home_acts.add(a); }
				else if (a.getType().equals("work")) { work_acts.add(a); }
				else { other_acts.add(a); }
			}
		}

		// set exactly one home and four other activities for each person
		Iterator<PersonImpl> p_it = plans.getPersons().values().iterator();
		while (p_it.hasNext()) {
			PersonImpl p = p_it.next();
			Knowledge k = this.knowledges.getFactory().createKnowledge(p.getId(), "created by " + this.getClass().getName());
			int index = MatsimRandom.getRandom().nextInt(home_acts.size());
			k.addActivity(home_acts.get(index),true);
			index = MatsimRandom.getRandom().nextInt(work_acts.size());
			k.addActivity(work_acts.get(index),true);
			for (int i=0; i<4; i++) {
				index = MatsimRandom.getRandom().nextInt(other_acts.size());
				k.addActivity(other_acts.get(index),false);
			}
		}

		System.out.println("    done.");
	}
}
