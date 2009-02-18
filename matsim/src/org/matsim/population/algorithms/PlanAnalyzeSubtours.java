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
import org.matsim.facilities.Facility;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.population.Act;
import org.matsim.population.Plan;

/**
 * Analyses plans for subtours. 
 * 
 * A subtour is a sequence of activities whose first 
 * and the last activity are at the same location.
 * The current implementation uses the {@link Facility} as location information, 
 * thus activities at the same facility constitute a subtour.
 * <br><br>
 * Usage:
 * <br><br>
 * <code>
 * Plan plan = ...;<br>
 * <br>
 * PlanAnalyzeSubtours past = new PlanAnalyzeSubtours();<br>
 * testee.run(plan);<br>
 * <br>
 * int numSubtours = past.getNumSubtours();<br>
 * int[] subtourIndexation = past.getSubtourIndexation();<br>
 * </code>
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

	public void run(Plan plan) {

		locationIds = new ArrayList<Id>();

		ArrayList<Object> actsLegs = plan.getActsLegs();
		for (int ii=0; ii < actsLegs.size(); ii++) {
			if (actsLegs.get(ii).getClass().equals(Act.class)) {
				// TODO subtour analysis should be possible on link-level, too
				// not only on facility level
				// in this case one could run a scenario also without facility information
				locationIds.add(((Act) actsLegs.get(ii)).getFacility().getId());
			}
		}

		this.numSubtours = 0;

		this.subtourIndexation = new int[locationIds.size() - 1];
		for (int ii = 0; ii < this.subtourIndexation.length; ii++) {
			this.subtourIndexation[ii] = PlanAnalyzeSubtours.UNDEFINED;
		}

		ArrayList<Id> locationEnumerator = new ArrayList<Id>();

		int ii = 0;
		while(ii <= locationIds.size() - 1) {
			Id currentLinkId = locationIds.get(ii);
			if (locationEnumerator.contains(currentLinkId)) {
				int lastLinkIndex = locationEnumerator.lastIndexOf(currentLinkId);
				for (int jj = lastLinkIndex; jj < ii; jj++) {
					if (this.subtourIndexation[jj] == PlanAnalyzeSubtours.UNDEFINED) {
						this.subtourIndexation[jj] = numSubtours;
					}
				}
				numSubtours++;
				for (int removeMe = lastLinkIndex; removeMe < ii; removeMe++) {
					locationEnumerator.set(removeMe, INVALID_ID);
				}
			}
			locationEnumerator.add(currentLinkId);
			ii++;
		}

	}

	/**
	 * Use this method to get information which leg belongs to which subtour.
	 * <br>
	 * <br>
	 * Example:<br>
	 * <br>
	 * Imagine an activity plan whose activities are located at {@link Facility}s with the following ids, in this order:<br>
	 * <br>
	 * 1 2 1 2 3 2 1<br>
	 * <br>
	 * Three subtours can be identified:<br>
	 * <ul>
	 * <li> One from the first activity at facility #1 to the second activity at facility #1.
	 * <li> One from the second activity at facility #1 to the last activity at facility #1, excluding the activities/legs in between.
	 * <li> One from the first activity at facility #2 to the last activity at facility #2.
	 * </ul>
	 * <br>
	 * The returned result in this case is an array of size 6 containing the numbers:<br>
	 * <br>
	 * 0 0 2 1 1 2<br>
	 * <br>
	 * The subtour analysis algorithm first identifies subtours lying within the plan. 
	 * This is why the "outer" subtour in the second part of the plan has the higher index than its "inner" part.<br>
	 * <br>
	 * For more illustrative examples, see the code of the test class {@link PlanAnalyzeSubtoursTest}.<br>  
	 * <br>
	 * @return an array with subtour indices [int] of each leg of the {@link Plan} that was analyzed most recently
	 */
	public int[] getSubtourIndexation() {
		return subtourIndexation;
	}

	/**
	 * Use this method to retrieve the number of subtours of an activity plan.
	 * <br>
	 * <br>
	 * Example:<br>
	 * <br>
	 * Imagine an activity plan whose activities are located at the following facility, in this order:<br>
	 * <br>
	 * 1 2 1 2 3 2 1<br>
	 * <br>
	 * Three subtours can be identified:<br>
	 * <ul>
	 * <li> One from the first activity at facility #1 to the second activity at facility #1.
	 * <li> One from the second activity at facility #1 to the last activity at facility #1, excluding the activities/legs in between.
	 * <li> One from the first activity at facility #2 to the last activity at facility #2.
	 * </ul>
	 * <br>
	 * The returned result in this case is:<br>
	 * <br>
	 * 3<br>
	 * <br>
	 * For more illustrative examples, see the code of the test class {@link PlanAnalyzeSubtoursTest}.<br>  
	 * <br>
	 * @return the number of subtours in the {@link Plan} that was analyzed most recently
	 */
	public int getNumSubtours() {
		return numSubtours;
	}

}
