/* *********************************************************************** *
 * project: org.matsim.*
 * JointTripsSelectorSolution.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.jointtrips.replanning.modules.jointtripsselector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.jointtrips.population.JointPlan;
import playground.thibautd.jointtrips.population.jointtrippossibilities.JointTripPossibilitiesUtils;
import playground.thibautd.jointtrips.population.jointtrippossibilities.JointTripPossibility;
import playground.thibautd.tsplanoptimizer.framework.Solution;
import playground.thibautd.tsplanoptimizer.framework.Value;
import playground.thibautd.tsplanoptimizer.framework.ValueImpl;

/**
 * @author thibautd
 */
public class JointTripsSelectorSolution implements Solution {
	private final PlanAlgorithm optimisationSubRoutine;
	private final JointPlan plan;
	// maintain two lists rather than a map, to be sure of the ordering
	private final List<JointTripPossibility> possibilities;
	private final List<Value<Boolean>> values;

	/**
	 * @param plan the plan under modification. The plan will not be cloned: the same instance
	 * is referenced and modified by all solutions!
	 * @param optimisationSubRoutine the algorithm to use to optimise
	 * durations and modes.
	 */
	public JointTripsSelectorSolution(
			final JointPlan plan,
			final PlanAlgorithm optimisationSubRoutine) {
		Map<JointTripPossibility, Value<Boolean>> possibilitiesMap = getValuesMap( plan );

		this.optimisationSubRoutine = optimisationSubRoutine;
		this.plan = plan;
		int size = possibilitiesMap.size();
		this.possibilities = new ArrayList<JointTripPossibility>( size );
		this.values = new ArrayList<Value<Boolean>>( size );

		for (Map.Entry<JointTripPossibility, Value<Boolean>> entry : possibilitiesMap.entrySet()) {
			possibilities.add( entry.getKey() );
			values.add( entry.getValue() );
		}
	}

	private JointTripsSelectorSolution(
			final JointPlan plan,
			final PlanAlgorithm optimisationSubRoutine,
			final List<JointTripPossibility> possibilities,
			final List<Value<Boolean>> values) {
		this.optimisationSubRoutine = optimisationSubRoutine;
		this.plan = plan;
		this.possibilities = possibilities;
		this.values = values;
	}

	@Override
	public List<? extends Value> getRepresentation() {
		return values;
	}

	@Override
	public Plan getRepresentedPlan() {
		Map<JointTripPossibility, Boolean> newPossibilities =
			 new HashMap<JointTripPossibility, Boolean>();

		Iterator<JointTripPossibility> possibilitiesIter = possibilities.iterator();
		Iterator<Value<Boolean>> valuesIter = values.iterator();

		while (possibilitiesIter.hasNext()) {
			newPossibilities.put(
					possibilitiesIter.next(),
					valuesIter.next().getValue());
		}

		JointTripPossibilitiesUtils.includeJointTrips(
				newPossibilities,
				plan);

		optimisationSubRoutine.run( plan );
		
		return plan;
	}

	@Override
	public Solution createClone() {
		List<Value<Boolean>> newValues = new ArrayList<Value<Boolean>>();

		for (Value<Boolean> value : values) {
			newValues.add( value.createClone() );
		}

		JointTripsSelectorSolution clone =
			new JointTripsSelectorSolution(
					plan,
					optimisationSubRoutine,
					possibilities,
					newValues);

		return clone;
	}

	@Override
	public boolean equals(final Object other) {
		if (other instanceof JointTripsSelectorSolution) {
			JointTripsSelectorSolution otherSolution =
				(JointTripsSelectorSolution) other;

			return possibilities.equals( otherSolution.possibilities );
		}

		return false;
	}

	@Override
	public int hashCode() {
		return possibilities.hashCode();
	}

	private static Map<JointTripPossibility, Value<Boolean>> getValuesMap(final JointPlan plan) {
		Map<JointTripPossibility, Value<Boolean>> possibilities = new HashMap<JointTripPossibility, Value<Boolean>>();

		for (Map.Entry<JointTripPossibility, Boolean> entry : JointTripPossibilitiesUtils.getPerformedJointTrips( plan ).entrySet()) {
			possibilities.put(
					entry.getKey(),
					new ValueImpl<Boolean>(entry.getValue()));
		}

		return possibilities;
	}
}

