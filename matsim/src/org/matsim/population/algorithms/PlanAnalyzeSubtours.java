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

import org.matsim.basic.v01.IdImpl;
import org.matsim.config.groups.PlanomatConfigGroup;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Activity;
import org.matsim.interfaces.core.v01.Plan;

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

	public PlanAnalyzeSubtours() {
		super();
	}

	public void run(final Plan plan) {

		this.locationIds = new ArrayList<Id>();

		PlanomatConfigGroup.TripStructureAnalysisLayerOption subtourAnalysisLocationType = Gbl.getConfig().planomat().getTripStructureAnalysisLayer();

		Id locationId = null;
		ArrayList<Object> actsLegs = plan.getPlanElements();
		for (int ii=0; ii < actsLegs.size(); ii++) {
			if (actsLegs.get(ii) instanceof Activity) {
				if (PlanomatConfigGroup.TripStructureAnalysisLayerOption.facility.equals(subtourAnalysisLocationType)) {
					locationId = ((Activity) actsLegs.get(ii)).getFacilityId();
				} else if (PlanomatConfigGroup.TripStructureAnalysisLayerOption.link.equals(subtourAnalysisLocationType)) {
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
			Id currentLinkId = this.locationIds.get(ii);
			if (locationEnumerator.contains(currentLinkId)) {
				int lastLinkIndex = locationEnumerator.lastIndexOf(currentLinkId);
				for (int jj = lastLinkIndex; jj < ii; jj++) {
					if (this.subtourIndexation[jj] == PlanAnalyzeSubtours.UNDEFINED) {
						this.subtourIndexation[jj] = this.numSubtours;
					}
				}
				this.numSubtours++;
				for (int removeMe = lastLinkIndex; removeMe < ii; removeMe++) {
					locationEnumerator.set(removeMe, INVALID_ID);
				}
			}
			locationEnumerator.add(currentLinkId);
			ii++;
		}

	}

	/**
	 * Use this method to get information which leg belongs to which subtour. See documentation <a href="http://matsim.org/node/264">here</a>.
	 * 
	 * @return an array with subtour indices [int] of each leg of the {@link Plan} that was analyzed most recently
	 */
	public int[] getSubtourIndexation() {
		return this.subtourIndexation;
	}

	/**
	 * Use this method to retrieve the number of subtours of an activity plan. See documentation <a href="http://matsim.org/node/264">here</a>.
	 * 
	 * @return the number of subtours in the {@link Plan} that was analyzed most recently
	 */
	public int getNumSubtours() {
		return this.numSubtours;
	}

}
