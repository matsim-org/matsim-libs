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

import java.util.NoSuchElementException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.router.priorityqueue.BinaryMinHeap;
import org.matsim.core.router.priorityqueue.MinHeap;
import playground.thibautd.socnetsim.replanning.grouping.ReplanningGroup;
import playground.thibautd.socnetsim.replanning.selectors.WeightCalculator;

final class PointingAgent {
	private final Id id;
	private final PlanRecord[] records;

	private final MinHeap<PlanRecord> heap;

	public PointingAgent(
			final Person person,
			final ReplanningGroup group,
			final WeightCalculator weight) {
		this.id = person.getId();
		this.records = new PlanRecord[ person.getPlans().size() ];
		this.heap = new BinaryMinHeap<PlanRecord>( records.length );

		int i = 0;
		for ( Plan p : person.getPlans() ) {
			records[ i ] =
				new PlanRecord(
						this,
						p,
						weight.getWeight(
							p,
							group ),
						i);
			this.heap.add(
					records[ i ],
					// inverse priority: we want decreasing order
					-records[ i ].getWeight() );
			i++;
		}
	}

	public PlanRecord[] getRecords() {
		return records;
	}

	public Plan getPointedPlan() {
		try {
			while ( !heap.peek().isFeasible() ) {
				heap.poll();
			}
		}
		catch ( NoSuchElementException e ) {
			throw new RuntimeException(
					"no more feasible plans for agent "+id,
					e );
		}

		return heap.peek().getPlan();
	}
}
