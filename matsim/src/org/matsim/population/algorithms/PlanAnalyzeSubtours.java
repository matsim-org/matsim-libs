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

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.population.Act;
import org.matsim.population.Plan;

/**
 * Analyses plans for subtours. 
 * 
 * A subtour is a sequence of activities whose first 
 * and the last activity are at the same location.
 * The current implementation uses the {@link Link} as location information, 
 * thus activities at the same link constitute a subtour.
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

	private static Logger log = Logger.getLogger(PlanAnalyzeSubtours.class);

	public PlanAnalyzeSubtours() {
		super();
	}

	public void run(Plan plan) {

		locationIds = new ArrayList<Id>();

		ArrayList<Object> actsLegs = plan.getActsLegs();
		for (int ii=0; ii < actsLegs.size(); ii++) {
			if (actsLegs.get(ii).getClass().equals(Act.class)) {
				locationIds.add(((Act) actsLegs.get(ii)).getLinkId());
			}
		}

		this.numSubtours = 0;

//		this.subtours = new TreeSet<Integer>();
		this.subtourIndexation = new int[locationIds.size() - 1];
		for (int ii = 0; ii < this.subtourIndexation.length; ii++) {
			this.subtourIndexation[ii] = PlanAnalyzeSubtours.UNDEFINED;
		}

		this.extractSubtours(0, locationIds.size() - 1);

	}

	protected void extractSubtours(final int startIndex, final int endIndex) {

		log.info("startIndex: " + startIndex);
		log.info("endIndex: " + endIndex);

		ArrayList<Id> locationEnumerator = new ArrayList<Id>();

		int ii = startIndex;
		while(ii <= endIndex) {
			Id currentLinkId = locationIds.get(ii);
			if (locationEnumerator.contains(currentLinkId)) {
				int lastLinkIndex = locationEnumerator.lastIndexOf(currentLinkId) + startIndex;
				for (int jj = lastLinkIndex; jj < ii; jj++) {
					if (this.subtourIndexation[jj] == PlanAnalyzeSubtours.UNDEFINED) {
						this.subtourIndexation[jj] = numSubtours;
					}
				}
				numSubtours++;
				if (this.subtourIndexation[lastLinkIndex] == PlanAnalyzeSubtours.UNDEFINED) {
					log.info("Calling extractSubtours(...) from " + (lastLinkIndex + 1) + " to " + (ii - 1));
					this.extractSubtours((lastLinkIndex + 1), (ii - 1));
				}
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
	 * Imagine an activity plan whose activities are located at {@link Link}s with the following ids, in this order:<br>
	 * <br>
	 * 1 2 1 2 3 2 1<br>
	 * <br>
	 * Three subtours can be identified:<br>
	 * <ul>
	 * <li> One from the first activity at link #1 to the second activity at link #1.
	 * <li> One from the second activity at link #1 to the last activity at link #1, excluding the activities/legs in between.
	 * <li> One from the first activity at link #2 to the last activity at link #2.
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
	 * Imagine an activity plan whose activities are located at the following links, in this order:<br>
	 * <br>
	 * 1 2 1 2 3 2 1<br>
	 * <br>
	 * Three subtours can be identified:<br>
	 * <ul>
	 * <li> One from the first activity at link #1 to the second activity at link #1.
	 * <li> One from the second activity at link #1 to the last activity at link #1, excluding the activities/legs in between.
	 * <li> One from the first activity at link #2 to the last activity at link #2.
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
