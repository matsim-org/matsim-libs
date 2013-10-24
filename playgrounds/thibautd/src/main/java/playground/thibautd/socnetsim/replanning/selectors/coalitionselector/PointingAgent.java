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

package playground.thibautd.socnetsim.replanning.selectors.coalitionselector;

import java.util.Arrays;
import java.util.Comparator;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

import playground.thibautd.socnetsim.replanning.grouping.ReplanningGroup;
import playground.thibautd.socnetsim.replanning.selectors.WeightCalculator;

final class PointingAgent {
	private static final Comparator<PlanRecord> DECREASING_COMP =
		new  Comparator<PlanRecord>() {
			@Override
			public int compare(
					final PlanRecord o1,
					final PlanRecord o2) {
				return Double.compare( o2.getWeight() , o1.getWeight() );
			}
		};

	private final Id id;
	private final PlanRecord[] records;
	private int pointer = 0;

	public PointingAgent(
			final Person person,
			final ReplanningGroup group,
			final WeightCalculator weight) {
		this.id = person.getId();
		this.records = new PlanRecord[ person.getPlans().size() ];

		int c=0;
		for ( Plan p : person.getPlans() ) {
			records[ c++ ] =
				new PlanRecord(
						this,
						p,
						weight.getWeight(
							p,
							group ) );
		}
		Arrays.sort( records , DECREASING_COMP );
	}

	public PlanRecord[] getRecords() {
		return records;
	}

	public Plan getPointedPlan() {
		try {
			while ( !records[ pointer ].isFeasible() ) pointer++;
		}
		catch ( ArrayIndexOutOfBoundsException e ) {
			throw new RuntimeException(
					"no more feasible plans for agent "+id,
					e );
		}
		return records[ pointer ].getPlan();
	}
}
