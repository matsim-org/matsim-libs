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

import playground.johannes.synpop.data.Attributable;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.source.mid2008.processing.EpisodeTask;

/**
 * @author johannes
 *
 */
public class SetActivityTimeTask implements EpisodeTask {

	/* (non-Javadoc)
	 * @see playground.johannes.synpop.source.mid2008.processing.EpisodeTask#apply(playground.johannes.synpop.data.PlainEpisode)
	 */
	@Override
	public void apply(Episode plan) {
		if(plan.getActivities().size() == 1) {
			Attributable act = plan.getActivities().get(0);
			
			act.setAttribute(CommonKeys.ACTIVITY_START_TIME, "0");
			act.setAttribute(CommonKeys.ACTIVITY_END_TIME, "86400");
		} else {
			
		
		for(int i = 0; i < plan.getActivities().size(); i++) {
			String startTime = "0";
			String endTime = "86400";
			
			Attributable act = plan.getActivities().get(i);
			
			if(i > 0) {
				Attributable prev = plan.getLegs().get(i-1);
				startTime = prev.getAttribute(CommonKeys.LEG_END_TIME);
				
				if(startTime != null) {
					/*
					 * set end time to 86400 or later if specified
					 */
					int start = Integer.parseInt(startTime);
					int end = Math.max(start + 1, 86400);
					endTime = String.valueOf(end);
				}
			}
			
			if(i < plan.getActivities().size() - 1) {
				Attributable next = plan.getLegs().get(i);
				endTime = next.getAttribute(CommonKeys.LEG_START_TIME);
			}
			
//			if(Double.parseDouble(endTime) < Double.parseDouble(startTime))
//				throw new RuntimeException();
			
			act.setAttribute(CommonKeys.ACTIVITY_START_TIME, startTime);
			act.setAttribute(CommonKeys.ACTIVITY_END_TIME, endTime);
		}
		}
	}

}
