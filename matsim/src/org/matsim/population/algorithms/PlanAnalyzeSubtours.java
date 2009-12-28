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

package org.matsim.population.algorithms;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.core.config.groups.PlanomatConfigGroup.TripStructureAnalysisLayerOption;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;

/**
 * Analyses plans for subtours. See documentation <a href="http://matsim.org/node/266">here</a>.
 *
 * @see PlanAnalyzeSubtoursTest
 *
 * @author meisterk
 *
 */
public class PlanAnalyzeSubtours implements PlanAlgorithm {

	public static final int UNDEFINED = Integer.MIN_VALUE;
	private static final Id INVALID_ID = new IdImpl(PlanAnalyzeSubtours.UNDEFINED);
	
	private int[] subtourIndexation = null;
	private ArrayList<Id> locationIds = null;
	private int numSubtours = Integer.MIN_VALUE;
	private List<List<PlanElement>> subTours;
	private List<Integer> parentTourIndices;
	private List<Integer> firstIndexOfSubtours;
	private List<Integer> lastIndexOfSubtours;
	private PlanomatConfigGroup planomatConfigGroup;

	public PlanAnalyzeSubtours(PlanomatConfigGroup planomatConfigGroup) {
		super();
		this.planomatConfigGroup = planomatConfigGroup;
	}

	public void run(final Plan plan) {

		this.locationIds = new ArrayList<Id>();
		this.subTours = new ArrayList<List<PlanElement>>();
		this.parentTourIndices = new ArrayList<Integer>();
		this.firstIndexOfSubtours = new ArrayList<Integer>();
		this.lastIndexOfSubtours = new ArrayList<Integer>();

		Id locationId = null;
		List<PlanElement> actsLegs = plan.getPlanElements();
		for (int ii=0; ii < actsLegs.size(); ii++) {
			if (actsLegs.get(ii) instanceof ActivityImpl) {
				TripStructureAnalysisLayerOption tripStructureAnalysisLayer = planomatConfigGroup.getTripStructureAnalysisLayer();
				if (PlanomatConfigGroup.TripStructureAnalysisLayerOption.facility.equals(tripStructureAnalysisLayer)) {
					locationId = ((ActivityImpl) actsLegs.get(ii)).getFacilityId();
				} else if (PlanomatConfigGroup.TripStructureAnalysisLayerOption.link.equals(tripStructureAnalysisLayer)) {
					locationId = ((ActivityImpl) actsLegs.get(ii)).getLinkId();
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
				List<PlanElement> subTour = actsLegs.subList(firstIndexOfSubtour, lastIndexOfSubtour);
				this.firstIndexOfSubtours.add(firstIndexOfSubtour);
				this.lastIndexOfSubtours.add(lastIndexOfSubtour);
				this.parentTourIndices.add(null);
				this.subTours.add(subTour);
				this.numSubtours++;
				for (int removeMe = lastLinkIndex; removeMe < ii; removeMe++) {
					locationEnumerator.set(removeMe, INVALID_ID);
				}
			}
			locationEnumerator.add(currentLocationId);
			ii++;
		}

	}

	/**
	 * Use this method to get information which leg belongs to which subtour. See documentation <a href="http://matsim.org/node/264">here</a>.
	 * 
	 * @return an array with subtour indices [int] of each leg of the {@link PlanImpl} that was analyzed most recently
	 */
	public int[] getSubtourIndexation() {
		return this.subtourIndexation;
	}

	/**
	 * Use this method to retrieve the number of subtours of an activity plan. See documentation <a href="http://matsim.org/node/264">here</a>.
	 * 
	 * @return the number of subtours in the {@link PlanImpl} that was analyzed most recently
	 */
	public int getNumSubtours() {
		return this.numSubtours;
	}
	
	public List<List<PlanElement>> getSubtours() {
		return this.subTours;
	}
	
	public List<Integer> getParentTours() {
		return this.parentTourIndices;
	}

	public List<Integer> getFromIndexOfSubtours() {
		return firstIndexOfSubtours;
	}

	public List<Integer> getToIndexOfSubtours() {
		return lastIndexOfSubtours;
	}

}
