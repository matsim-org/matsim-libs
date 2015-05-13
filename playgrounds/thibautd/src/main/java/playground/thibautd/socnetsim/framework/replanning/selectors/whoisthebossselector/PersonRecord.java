/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.thibautd.socnetsim.framework.replanning.selectors.whoisthebossselector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

final class PersonRecord {
	final Person person;
	final List<PlanRecord> plans;
	final List<PlanRecord> prunedPlans;

	public PersonRecord(
			final Person person,
			final List<PlanRecord> plans) {
		this.person = person;
		this.plans = plans;
		Collections.sort(
				this.plans,
				new Comparator<PlanRecord>() {
					@Override
					public int compare(
							final PlanRecord o1,
							final PlanRecord o2) {
						// sort in DECREASING order
						return Double.compare( o2.individualPlanWeight , o1.individualPlanWeight );
					}
				});
		this.prunedPlans = new ArrayList<PlanRecord>( plans );
	}

	public PlanRecord getRecord( final Plan plan ) {
		for (PlanRecord r : plans) {
			if (r.plan == plan) return r;
		}
		throw new IllegalArgumentException();
	}

	@Override
	public String toString() {
		return "{PersonRecord: person="+person+"; plans="+plans+"}";
	}
}
