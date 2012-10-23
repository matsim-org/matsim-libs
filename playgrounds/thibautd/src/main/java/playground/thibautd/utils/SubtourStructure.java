/* *********************************************************************** *
 * project: org.matsim.*
 * SubtourStructure.java
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
package playground.thibautd.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.population.algorithms.PlanAnalyzeSubtours;

/**
 * Attempt at cleaning the subtour analysis interface.
 * @author thibautd
 */
public class SubtourStructure {
	private static final int UNDEFINED = Integer.MIN_VALUE;
	private static final Id INVALID_ID = new IdImpl(UNDEFINED);

	private final List<Subtour> subtours = new ArrayList<Subtour>();

	public SubtourStructure(final Plan plan) {
		this( plan.getPlanElements() );
	}

	public SubtourStructure(final List<PlanElement> actsLegs) {
		List<Id> linkIds = new ArrayList<Id>();
		List<List<PlanElement>> subtourElements = new ArrayList<List<PlanElement>>();
		List<Integer> parentTourIndices = new ArrayList<Integer>();
		List<Integer> firstIndexOfSubtours = new ArrayList<Integer>();
		List<Integer> lastIndexOfSubtours = new ArrayList<Integer>();

		for (PlanElement pe : actsLegs) {
			if (pe instanceof Activity) {
				linkIds.add( ((Activity) pe).getLinkId() );
			}
		}

		int numSubtours = 0;

		int[] subtourIndexation = new int[linkIds.size() - 1];
		for (int ii = 0; ii < subtourIndexation.length; ii++) {
			subtourIndexation[ii] = PlanAnalyzeSubtours.UNDEFINED;
		}

		ArrayList<Id> locationEnumerator = new ArrayList<Id>();

		int ii = 0;
		while(ii <= linkIds.size() - 1) {
			Id currentLocationId = linkIds.get(ii);
			if (locationEnumerator.contains(currentLocationId)) {
				int lastLinkIndex = locationEnumerator.lastIndexOf(currentLocationId);

				for (int jj = lastLinkIndex; jj < ii; jj++) {
					if (subtourIndexation[jj] == PlanAnalyzeSubtours.UNDEFINED) {
						subtourIndexation[jj] = numSubtours;
					} else {
						if (parentTourIndices
								.get(subtourIndexation[jj]) == null) {
							parentTourIndices.set(
									subtourIndexation[jj], numSubtours);
						}
					}
				}
				int firstIndexOfSubtour = 2 * lastLinkIndex;
				int lastIndexOfSubtour = 2 * ii;
				List<PlanElement> subTour = actsLegs.subList(firstIndexOfSubtour, lastIndexOfSubtour + 1);
				firstIndexOfSubtours.add(firstIndexOfSubtour);
				lastIndexOfSubtours.add(lastIndexOfSubtour);
				parentTourIndices.add(null);
				subtourElements.add(subTour);
				numSubtours++;
				for (int removeMe = lastLinkIndex; removeMe < ii; removeMe++) {
					locationEnumerator.set(removeMe, INVALID_ID);
				}
			}
			locationEnumerator.add(currentLocationId);
			ii++;
		}

		for (int i=0; i < subtourElements.size(); i++) {
			subtours.add( new Subtour(
						subtourElements.get( i ),
						firstIndexOfSubtours.get( i ),
						lastIndexOfSubtours.get( i )));
		}

		for (int i=0; i < subtourElements.size(); i++) {
			int parent = parentTourIndices.get( i );
			if (parent != UNDEFINED) {
				subtours.get( i ).parent = subtours.get( parent );
			}
		}
	}

	public List<Subtour> getSubtours() {
		return subtours;
	}

	public static class Subtour {
		private final List<PlanElement> planElements;
		private final int indexOfFirstElementInPlan;
		private final int indexOfLastElementInPlan;
		private Subtour parent = null;

		private Subtour(
				final List<PlanElement> planElements,
				final int indexOfFirstElementInPlan,
				final int indexOfLastElementInPlan) {
			this.planElements = Collections.unmodifiableList( planElements );
			this.indexOfFirstElementInPlan = indexOfFirstElementInPlan;
			this.indexOfLastElementInPlan = indexOfLastElementInPlan;
		}

		public List<PlanElement> getPlanElements() {
			return this.planElements;
		}

		public int getIndexOfFirstElementInPlan() {
			return this.indexOfFirstElementInPlan;
		}

		public int getIndexOfLastElementInPlan() {
			return this.indexOfLastElementInPlan;
		}

		public Subtour getParent() {
			return this.parent;
		}
	}
}
