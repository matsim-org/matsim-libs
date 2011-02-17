/* *********************************************************************** *
 * project: org.matsim.*
 * EditRoutes.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.christoph.withinday.utils;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;

/*
 * Only the PlanElements are changed - further Steps
 * like updating the Routes of the previous and next Leg
 * have to be done elsewhere.
 */
public class ReplacePlanElements {

	public boolean replaceActivity(Plan plan, Activity oldActivity, Activity newActivity) {
		if (oldActivity == null) return false;
		if (newActivity == null) return false;
		
		int index = plan.getPlanElements().indexOf(oldActivity);
		// yyyy I can't say how safe this is.  There is no guarantee that the same entry is not used twice in the plan.  This will in
		// particular be a problem if we override the "equals" contract, in the sense that two activities are equal if
		// certain (or all) elements are equal.  kai, oct'10
		
		if (index == -1) return false;
		
//		/*
//		 *  If the new Activity takes place on a different Link
//		 *  we have to replan the Routes from an to that Activity.
//		 */
//		if (oldActivity.getLinkId() != newActivity.getLinkId())
//		{
//			
//		}
		
		plan.getPlanElements().remove(index);
		plan.getPlanElements().add(index, newActivity);
		
		return true;
	}
	
	public boolean replaceLeg(Plan plan, Leg oldLeg, Leg newLeg) {
		if (oldLeg == null) return false;
		if (newLeg == null) return false;
		
		int index = plan.getPlanElements().indexOf(oldLeg);
		// yyyy I can't say how safe this is.  There is no guarantee that the same entry is not used twice in the plan.  This will in
		// particular be a problem if we override the "equals" contract, in the sense that two legs are equal if
		// certain (or all) elements are equal.  kai, oct'10
		
		if (index == -1) return false;
		
		plan.getPlanElements().remove(index);
		plan.getPlanElements().add(index,newLeg);
		
		return true;
	}
}
