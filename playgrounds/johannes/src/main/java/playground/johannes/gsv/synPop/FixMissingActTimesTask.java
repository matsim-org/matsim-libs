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

import playground.johannes.synpop.data.Element;

/**
 * @author johannes
 *
 */
public class FixMissingActTimesTask implements ProxyPlanTask {

	private static final int MIN_LEG_DURATION = 1;
	
	private static final int MIN_ACT_DURATION = 1;

	@Override
	public void apply(ProxyPlan plan) {
		for(int i = 0; i < plan.getActivities().size(); i++) {
			Element act = plan.getActivities().get(i);
			
			if(act.getAttribute(CommonKeys.ACTIVITY_START_TIME) == null) {
				
				if(i > 0) {
					Element prev = plan.getActivities().get(i - 1);
					String timeStr = prev.getAttribute(CommonKeys.ACTIVITY_END_TIME);
					if(timeStr != null) {
						int prevEndTime = Integer.parseInt(timeStr); 
						act.setAttribute(CommonKeys.ACTIVITY_START_TIME, String.valueOf(prevEndTime + MIN_LEG_DURATION));
					} else
						throw new RuntimeException("Insufficient information. End time of previous activity not set.");
					
				} else {
					act.setAttribute(CommonKeys.ACTIVITY_START_TIME, "0");
				}
			}
			
			if(act.getAttribute(CommonKeys.ACTIVITY_END_TIME) == null) {
				
				if(i < plan.getActivities().size() - 2) {
					Element next = plan.getActivities().get(i + 1);
					String timeStr = next.getAttribute(CommonKeys.ACTIVITY_START_TIME);
					
					if(timeStr != null) {
						int nextStartTime = Integer.parseInt(timeStr) ;
						act.setAttribute(CommonKeys.ACTIVITY_END_TIME, String.valueOf(nextStartTime - MIN_LEG_DURATION));
					} else {
//						throw new RuntimeException("Insufficient information. Start time of next activity not set.");
						int start = Integer.parseInt(act.getAttribute(CommonKeys.ACTIVITY_START_TIME));
						act.setAttribute(CommonKeys.ACTIVITY_END_TIME, String.valueOf(start + MIN_ACT_DURATION));
					}
				} else {
					act.setAttribute(CommonKeys.ACTIVITY_END_TIME, "86400");
				}
			}
		}

	}

}
