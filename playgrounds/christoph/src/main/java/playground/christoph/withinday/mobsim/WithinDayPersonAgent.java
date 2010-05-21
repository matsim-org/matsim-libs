/* *********************************************************************** *
 * project: org.matsim.*
 * WithinDayPersonAgent.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.christoph.withinday.mobsim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.ptproject.qsim.QPersonAgent;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.QSimI;
import org.matsim.core.population.PersonImpl;

import playground.christoph.withinday.replanning.WithinDayReplanner;

public class WithinDayPersonAgent extends QPersonAgent{

	private static final Logger log = Logger.getLogger(WithinDayPersonAgent.class);

	private List<WithinDayReplanner> withinDayReplanner = new ArrayList<WithinDayReplanner>();
	
	public WithinDayPersonAgent(final PersonImpl p, final QSimI simulation)
	{
		super(p, simulation);
	}

	/*
	 * Resets cached next Link. If a Person is in the Waiting Queue to leave a
	 * Link he/she may replan his/her Route so the cached Link would be wrong.
	 * 
	 * This should be more efficient that resetting it in chooseNextLink()
	 * because it can be called from the Replanning Module and isn't done for
	 * every Agent even it is not necessary.
	 */
	public void resetCachedNextLink()
	{
		super.cachedNextLinkId = null;
	}

	/*
	 * Returns the currently performed Activity or the
	 * Activity that will be performed after the current
	 * Leg. 
	 */
	public Activity getCurrentActivity()
	{
		Activity currentActivity = null;
		
		// The Person is currently at an Activity and is going to leave it.
		// The Person's CurrentLeg should point to the leg that leads to that Activity...
		List<PlanElement> planElements = this.getPerson().getSelectedPlan().getPlanElements();
		
		Leg currentLeg = this.getCurrentLeg();
		
		// first Activity - there is no previous Leg
		if (currentLeg == null)
		{
			currentActivity = (Activity)planElements.get(0);
		}
		else
		{
			int index = planElements.indexOf(currentLeg);
			// If the Leg is part of the Person's Plan
			if (index >= 0)
			{
				currentActivity = (Activity)planElements.get(index + 1);
			}
		}
		
		if (currentActivity == null)
		{
			log.error("Could not find Activity!");
		}
		
		return currentActivity;
	}
	
	public boolean addWithinDayReplanner(WithinDayReplanner replanner)
	{
		return this.withinDayReplanner.add(replanner);
	}
	
	public boolean removeWithinDayReplanner(WithinDayReplanner replanner)
	{
		return this.withinDayReplanner.remove(replanner);
	}
	
	public List<WithinDayReplanner> getWithinDayReplanners()
	{
		return Collections.unmodifiableList(withinDayReplanner);
	}
}