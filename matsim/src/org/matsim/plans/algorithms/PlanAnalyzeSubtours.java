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

package org.matsim.plans.algorithms;

import java.util.ArrayList;

import org.matsim.basic.v01.Id;
import org.matsim.plans.Act;
import org.matsim.plans.Plan;

/**
 * Analyses plans for subtours. 
 * 
 * A subtour is a sequence of activities where the first 
 * and the last activity are at the same location.
 * The current implementation uses the link as location information, 
 * thus activities at the same link constitute a subtour.
 * 
 * @author meisterk
 *
 */
public class PlanAnalyzeSubtours implements PlanAlgorithmI {

	private int numSubtours;

	/**
	 * Maps leg numbers to subtour ids.
	 */
	private int[] subtours = null; 

	private ArrayList<Id> locationIds = new ArrayList<Id>();

	public PlanAnalyzeSubtours() {
		super();
		this.numSubtours = Integer.MIN_VALUE;
	}

	public int getNumSubtours() {
		if (numSubtours == Integer.MIN_VALUE) {
			for (int legId = 0; legId < subtours.length; legId++) {
				if (subtours[legId] > numSubtours) {
					numSubtours = subtours[legId];
				}
			}
		}
		// number of subtours is highest subtour index + 1
		return numSubtours + 1;
	}

	public void run(Plan plan) {

		ArrayList<Object> actsLegs = plan.getActsLegs();
		for (int ii=0; ii < actsLegs.size(); ii+=2) {
			locationIds.add(((Act) actsLegs.get(ii)).getLinkId());
		}

		this.subtours = new int[locationIds.size()];

		this.extractSubtours(0, locationIds.size() - 1, 0);

		this.printSubtours();

	}

	protected void extractSubtours(final int startIndex, final int endIndex, final int subtourId) {

		if (startIndex == endIndex) {
			return;
		}

		subtours[startIndex] = subtourId;
		for (int ii = startIndex + 1; ii <= endIndex; ii++) {
			subtours[ii] = subtourId;
			// subtour found
			if (locationIds.get(ii).equals(locationIds.get(startIndex))) {
				// analyse locations in between for subtours
				this.extractSubtours(startIndex + 1, ii - 1, subtourId + 1);
			}
		}

	}

	protected void printSubtours() {

		for (int legId = 0; legId < subtours.length; legId++) {
			System.out.println(legId + "->" + subtours[legId]);
		}

	}

}
