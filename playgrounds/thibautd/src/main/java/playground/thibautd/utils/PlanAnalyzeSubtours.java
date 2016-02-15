/* *********************************************************************** *
 * project: org.matsim.*
 * PlanAnalyzeSubtours.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.TripStructureUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Analyses plans for subtours. See documentation <a href="http://matsim.org/node/266">here</a>.
 *
 * @deprecated the functionality of this thing now lies in {@link TripStructureUtils#getSubtours()}.
 * @see PlanAnalyzeSubtoursTest
 *
 * @author meisterk
 *
 */
@Deprecated
public class PlanAnalyzeSubtours {

	public static final int UNDEFINED = Integer.MIN_VALUE;
	private static final Id<Object> INVALID_ID = Id.create(PlanAnalyzeSubtours.UNDEFINED, Object.class);

	// former fields. should become local variable in analysis.
	private int[] subtourIndexation;
	private ArrayList<Id> locationIds;
	private int numSubtours = Integer.MIN_VALUE;
	private List<List<PlanElement>> subtourElements;
	private List<Integer> parentTourIndices;
	private List<Integer> firstIndexOfSubtours;
	private List<Integer> lastIndexOfSubtours;
	// only this should remain:
	private List<Subtour> subtours;

	public PlanAnalyzeSubtours( final Plan plan ) {
		this( plan , false );
	}

	public PlanAnalyzeSubtours( final List<PlanElement> actsLegs ) {
		this( actsLegs , false );
	}

	public PlanAnalyzeSubtours(
			final Plan plan,
			final boolean useFacilitiesInsteadOfLinks) {
		this( plan.getPlanElements() , useFacilitiesInsteadOfLinks );
	}

	public PlanAnalyzeSubtours(
			final List<PlanElement> actsLegs,
			final boolean useFacilitiesInsteadOfLinks) {
		this.locationIds = new ArrayList<Id>();
		this.subtourElements = new ArrayList<List<PlanElement>>();
		this.parentTourIndices = new ArrayList<Integer>();
		this.firstIndexOfSubtours = new ArrayList<Integer>();
		this.lastIndexOfSubtours = new ArrayList<Integer>();
		this.subtours = new ArrayList<Subtour>();

		Id locationId = null;
		for (int ii=0; ii < actsLegs.size(); ii++) {
			if (actsLegs.get(ii) instanceof Activity) {
				if ( useFacilitiesInsteadOfLinks ) {
					locationId = ((Activity) actsLegs.get(ii)).getFacilityId();
				}
				else {
					locationId = ((Activity) actsLegs.get(ii)).getLinkId();
				}
				this.locationIds.add(locationId);
			}
		}

		this.numSubtours = 0;

		this.subtourIndexation = new int[this.locationIds.size() - 1];
		for (int ii = 0; ii < this.subtourIndexation.length; ii++) {
			this.subtourIndexation[ii] = PlanAnalyzeSubtours.UNDEFINED;
		}

		ArrayList<Id> locationEnumerator = new ArrayList<Id>();

		int ii = 0;
		while(ii <= this.locationIds.size() - 1) {
			Id currentLocationId = this.locationIds.get(ii);
			if (locationEnumerator.contains(currentLocationId)) {
				int lastLinkIndex = locationEnumerator
						.lastIndexOf(currentLocationId);
				for (int jj = lastLinkIndex; jj < ii; jj++) {
					if (this.subtourIndexation[jj] == PlanAnalyzeSubtours.UNDEFINED) {
						this.subtourIndexation[jj] = this.numSubtours;
					} else {
						if (this.parentTourIndices
								.get(this.subtourIndexation[jj]) == null) {
							this.parentTourIndices.set(
									this.subtourIndexation[jj], numSubtours);
						}
					}
				}
				int firstIndexOfSubtour = 2 * lastLinkIndex;
				int lastIndexOfSubtour = 2 * ii;
				List<PlanElement> subTour = actsLegs.subList(firstIndexOfSubtour, lastIndexOfSubtour + 1);
				this.firstIndexOfSubtours.add(firstIndexOfSubtour);
				this.lastIndexOfSubtours.add(lastIndexOfSubtour);
				this.parentTourIndices.add(null);
				this.subtourElements.add(subTour);
				this.numSubtours++;
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
			Integer parent = parentTourIndices.get( i );
			if (parent != null) {
				subtours.get( i ).parent = subtours.get( parent );
			}
		}
	}

	/**
	 * Use this method to get information which leg belongs to which subtour. See documentation <a href="http://matsim.org/node/264">here</a>.
	 *
	 * @return an array with subtour indices [int] of each leg of the {@link PlanImpl} that was analyzed most recently
	 */
	@Deprecated
	public int[] getSubtourIndexation() {
		return this.subtourIndexation;
	}

	/**
	 * Use this method to retrieve the number of subtours of an activity plan. See documentation <a href="http://matsim.org/node/264">here</a>.
	 *
	 * @return the number of subtours in the {@link PlanImpl} that was analyzed most recently
	 */
	@Deprecated
	public int getNumSubtours() {
		return this.numSubtours;
	}

	@Deprecated
	public List<List<PlanElement>> getSubtourElements() {
		return this.subtourElements;
	}

	@Deprecated
	public List<Integer> getParentTours() {
		return this.parentTourIndices;
	}

	@Deprecated
	public List<Integer> getFromIndexOfSubtours() {
		return firstIndexOfSubtours;
	}

	@Deprecated
	public List<Integer> getToIndexOfSubtours() {
		return lastIndexOfSubtours;
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
