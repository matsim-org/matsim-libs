/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop;

/**
 * @author johannes
 *
 */
public class SetActivityTimeTask implements ProxyPlanTask {

	/* (non-Javadoc)
	 * @see playground.johannes.gsv.synPop.ProxyPlanTask#apply(playground.johannes.gsv.synPop.ProxyPlan)
	 */
	@Override
	public void apply(ProxyPlan plan) {
		if(plan.getActivities().size() == 1) {
			ProxyActivity act = plan.getActivities().get(0);
			
			act.setAttribute(CommonKeys.ACTIVITY_START_TIME, 0);
			act.setAttribute(CommonKeys.ACTIVITY_END_TIME, 86400);
		} else {
			
		
		for(int i = 0; i < plan.getActivities().size(); i++) {
			Integer startTime = 0;
			Integer endTime = 86400;
			
			ProxyActivity act = plan.getActivities().get(i);
			
			if(i > 0) {
				ProxyLeg prev = plan.getLegs().get(i-1);
				startTime = (Integer) prev.getAttribute(CommonKeys.LEG_END_TIME);
			}
			
			if(i < plan.getActivities().size() - 1) {
				ProxyLeg next = plan.getLegs().get(i);
				endTime = (Integer) next.getAttribute(CommonKeys.LEG_START_TIME);
			}
			
			act.setAttribute(CommonKeys.ACTIVITY_START_TIME, startTime);
			act.setAttribute(CommonKeys.ACTIVITY_END_TIME, endTime);
		}
		}
	}

}
