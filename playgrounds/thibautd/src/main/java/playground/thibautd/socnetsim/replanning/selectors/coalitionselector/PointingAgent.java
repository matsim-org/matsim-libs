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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
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
	private final Collection<PlanRecord> records;

	private PlanRecord pointed = null;
	private final Iterator<PlanRecord> pointedIterator;

	public PointingAgent(
			final Person person,
			final ReplanningGroup group,
			final WeightCalculator weight) {
		this.id = person.getId();
		this.records = new ArrayList<PlanRecord>( person.getPlans().size() );

		int i = 0;
		for ( Plan p : person.getPlans() ) {
			records.add(
				new PlanRecord(
						this,
						p,
						weight.getWeight(
							p,
							group ),
						i++) );
		}

		pointedIterator = new SortingIterator( records );
		pointed = pointedIterator.next();
	}

	public Iterable<PlanRecord> getRecords() {
		return records;
	}

	public Plan getPointedPlan() {
		try {
			while ( !pointed.isFeasible() ) {
				pointed = pointedIterator.next();
			}
		}
		catch ( NoSuchElementException e ) {
			throw new RuntimeException(
					"no more feasible plans for agent "+id,
					e );
		}

		return pointed.getPlan();
	}
}

class SortingIterator implements Iterator<PlanRecord> {
	private final MinHeap<PlanRecord> heap;

	public SortingIterator(final Collection<PlanRecord> records) {
		this.heap = new BinaryMinHeap<PlanRecord>( records.size() );
		for ( PlanRecord r : records ) {
			this.heap.add(
					r,
					// inverse priority: we want decreasing order
					-r.getWeight() );
		}
	}

	@Override
	public boolean hasNext() {
		return !heap.isEmpty();
	}

	@Override
	public PlanRecord next() {
		final PlanRecord next = heap.poll();
		if ( next == null ) throw new NoSuchElementException();
		return next;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}
