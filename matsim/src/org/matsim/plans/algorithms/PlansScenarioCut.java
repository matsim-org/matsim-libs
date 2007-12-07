/* *********************************************************************** *
 * project: org.matsim.*
 * PlansScenarioCut.java
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

import org.matsim.gbl.Gbl;
import org.matsim.plans.Act;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.identifiers.IdI;

public class PlansScenarioCut extends PlansAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final double minX;
	private final double maxX;
	private final double minY;
	private final double maxY;

	//////////////////////////////////////////////////////////////////////
	// constructor
	//////////////////////////////////////////////////////////////////////

	public PlansScenarioCut(final CoordI min, final CoordI max) {
		super();
		this.minX = min.getX();
		this.maxX = max.getX();
		this.minY = min.getY();
		this.maxY = max.getY();
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(final Plans plans) {
		System.out.println("    running " + this.getClass().getName() + " module...");

		TreeSet<IdI> pid_set = new TreeSet<IdI>(); // ids of persons to remove
		Iterator<IdI> pid_it = plans.getPersons().keySet().iterator();
		while (pid_it.hasNext()) {
			IdI personId = pid_it.next();
			Person person = plans.getPerson(personId);
			if (person.getPlans().isEmpty()) {
				pid_set.add(personId);
			}
			else {
				if (person.getPlans().size() > 1) {
					Gbl.errorMsg("Module does handle Persons with more than one plan!");
				}
				Plan plan = person.getPlans().get(0);
				Iterator<?> a_it = plan.getIteratorAct();
				while (a_it.hasNext()) {
					Act a = (Act)a_it.next();
					CoordI coord = a.getCoord();
					if (coord == null) { Gbl.errorMsg("Module requires 'act coord'"); }
					double x = coord.getX();
					double y = coord.getY();
					if (!((x < this.maxX) && (this.minX < x) && (y < this.maxY) && (this.minY < y))) {
						pid_set.add(personId);
					}
				}
			}
		}

		System.out.println("      Number of persons to be cut = " + pid_set.size() + "...");
		pid_it = pid_set.iterator();
		while (pid_it.hasNext()) {
			IdI pid = pid_it.next();
			plans.getPersons().remove(pid);
		}
		System.out.println("      done.");

		System.out.println("    done.");
  }
}
