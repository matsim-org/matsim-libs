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

package org.matsim.plans.algorithms;

import java.util.Iterator;

import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;


public class PersonRemoveReferences extends PersonAlgorithm {

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
		if (person.getKnowledge() != null) {
			if(!person.getKnowledge().removeAllFacilities()) {
				Gbl.errorMsg("Could not remove all facilities!");
			}
		}
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
