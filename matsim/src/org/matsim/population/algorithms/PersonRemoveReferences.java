/* *********************************************************************** *
 * project: org.matsim.*
 * PersonRemoveReferences.java
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

import java.util.Iterator;

import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;


public class PersonRemoveReferences extends AbstractPersonAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonRemoveReferences() {
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(final Person person) {
		Iterator<Plan> p_it = person.getPlans().iterator();
		while (p_it.hasNext()) {
			Plan p = p_it.next();
			Iterator<?> l_it = p.getIteratorLeg();
			while (l_it.hasNext()) {
				Leg l = (Leg)l_it.next();
				l.setRoute(null);
			}

			Iterator<?> a_it = p.getIteratorAct();
			while (a_it.hasNext()) {
				Act a = (Act)a_it.next();
				Link link = a.getLink();
				if (link == null) {
					if (a.getCoord() == null) { Gbl.errorMsg("Something is worng!"); }
				}
				else {
					if (a.getCoord() != null) {
						a.setLink(null);
					}
					else {
						a.setCoord(a.getLink().getCenter());
						a.setLink(null);
					}
				}
			}
		}
	}
}
