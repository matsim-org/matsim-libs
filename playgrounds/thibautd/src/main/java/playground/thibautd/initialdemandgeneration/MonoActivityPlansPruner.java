/* *********************************************************************** *
 * project: org.matsim.*
 * MonoActivityPlansPruner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.initialdemandgeneration;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;

/**
 * Removes agents with mono-activity plans from a population.
 * it only looks at the selected plan.
 *
 * @author thibautd
 */
public class MonoActivityPlansPruner {
	public void run(final Population pop) {
		List<Id> toRemove = new ArrayList<Id>();
		for ( Person person : pop.getPersons().values() ) {
			if (person.getSelectedPlan().getPlanElements().size() == 1) {
				toRemove.add( person.getId() );
			}
		}

		for (Id id : toRemove) {
			pop.getPersons().remove( id );
		}
	}
}

