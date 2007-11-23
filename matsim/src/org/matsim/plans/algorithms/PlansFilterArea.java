/* *********************************************************************** *
 * project: org.matsim.*
 * PlansFilterArea.java
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

import java.util.Iterator;
import java.util.TreeSet;

import org.matsim.plans.Act;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.utils.identifiers.IdI;
import org.matsim.world.Coord;

/**
 * Filters out plans from persons where none of the act's locations are within
 * a user specified area. plans where at least one act happens within the area
 * are kept.<br/>
 * Persons with no plans left are removed from the population.
 */
public class PlansFilterArea extends PlansAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final double minX;
	private final double maxX;
	private final double minY;
	private final double maxY;

	public PlansFilterArea(final Coord min, final Coord max) {
		super();
		this.minX = min.getX();
		this.maxX = max.getX();
		this.minY = min.getY();
		this.maxY = max.getY();
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(final Plans plans) {
		System.out.println("    running " + this.getClass().getName() + " algorithm...");

		// first search all persons without plans
		TreeSet<IdI> pid_set = new TreeSet<IdI>();	// ids of persons to remove
		Iterator<IdI> pid_it = plans.getPersons().keySet().iterator();
		while (pid_it.hasNext()) {
			IdI personId = pid_it.next();
			Person person = plans.getPerson(personId);

			run(person);	// handle the person; in this step plans may get removed from a person
			if (person.getPlans().isEmpty()) {
				// the person has no plans left. remove the person afterwards (so we do not disrupt the Iterator)
				pid_set.add(personId);
			}
		}

		// okay, now remove in a 2nd step all persons we do no longer need
		pid_it = pid_set.iterator();
		while (pid_it.hasNext()) {
			IdI pid = pid_it.next();
			plans.getPersons().remove(pid);
		}

		System.out.println("    done.");
		System.out.println("Number of persons removed: " + pid_set.size());
	}

	private void run(final Person person) {
		for (int i=person.getPlans().size()-1; i>=0; i--) {
			int cntActs = 0;
			int cntOutside = 0;
			Plan plan = person.getPlans().get(i);

			for (int j=0; j<plan.getActsLegs().size(); j+=2) {
				Act act = (Act)plan.getActsLegs().get(j);
				cntActs++;
				double x = act.getCoord().getX();
				double y = act.getCoord().getY();
				if ((x < this.minX) || (x > this.maxX) || (y < this.minY) || (y > this.maxY)) {
					cntOutside++;
				}
			}

			if (cntActs == cntOutside) {
				// all acts are outside. plan is (most likely) not interesting for us
				person.getPlans().remove(i);
				i--;
			}
		}
	}

}
