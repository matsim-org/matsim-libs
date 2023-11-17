/* *********************************************************************** *
 * project: org.matsim.*
 * WorstPlanSelector.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.core.replanning.conflicts;

import java.util.Collections;
import java.util.LinkedList;

import org.apache.commons.lang3.tuple.Pair;
import org.jboss.logging.Logger;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.selectors.PlanSelector;

import com.google.inject.Inject;

/**
 * This selector is used like the standard WorstPlanForRemovalSelector to reduce
 * plans from an agent's memory. However, adhering to the conflict resolution
 * logic, the selector will make sure that a non-conflicting plan is never
 * selected for removal if it is the last remaining non-conflicting one. This
 * way, we make sure that every agent always keeps one plan that is not
 * conflicting.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class WorstPlanForRemovalSelectorWithConflicts implements PlanSelector<Plan, Person> {
	public static final String SELECTOR_NAME = "WorstPlanForRemovalSelectorWithConflicts";

	private final Logger logger = Logger.getLogger(WorstPlanForRemovalSelectorWithConflicts.class);

	private final ConflictManager conflictManager;

	@Inject
	public WorstPlanForRemovalSelectorWithConflicts(ConflictManager conflictManager) {
		this.conflictManager = conflictManager;
	}

	@Override
	public Plan selectPlan(HasPlansAndId<Plan, Person> person) {
		LinkedList<Pair<Plan, Double>> sorter = new LinkedList<>();
		int nonConflictingCount = 0;

		for (Plan plan : person.getPlans()) {
			double score = plan.getScore() == null ? Double.NEGATIVE_INFINITY : plan.getScore();
			sorter.add(Pair.of(plan, score));

			if (!conflictManager.isPotentiallyConflicting(plan)) {
				nonConflictingCount++;
			}
		}

		if (nonConflictingCount == 0) {
			logger.error(String.format("No non-conflicting plan found for agent %s", person.getId()));
		}

		Collections.sort(sorter, (a, b) -> Double.compare(a.getRight(), b.getRight()));

		if (nonConflictingCount == 1 && !conflictManager.isPotentiallyConflicting(sorter.getFirst().getLeft())) {
			// Remove the first from the removable candidates if it is the only
			// non-conflicting one
			sorter.removeFirst();
		}

		if (sorter.size() > 0) {
			return sorter.getFirst().getLeft();
		}

		return null;
	}

}
