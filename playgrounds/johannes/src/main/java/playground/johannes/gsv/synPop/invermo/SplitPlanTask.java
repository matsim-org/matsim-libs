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

package playground.johannes.gsv.synPop.invermo;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyPersonTask;
import playground.johannes.gsv.synPop.mid.MIDKeys;
import playground.johannes.synpop.data.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author johannes
 * 
 */
public class SplitPlanTask implements ProxyPersonTask {
	
	private static final Logger logger = Logger.getLogger(SplitPlanTask.class);

	public static DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm");

	@Override
	public void apply(PlainPerson person) {
		List<Episode> newPlans = new ArrayList<Episode>();
		
		for (Episode plan : person.getEpisodes()) {
			splitPlan(plan, newPlans);
		}

		person.getEpisodes().clear();
		
		for(Episode plan : newPlans)
			person.addEpisode(plan);
	}
	
	private void splitPlan(Episode plan, List<Episode> newPlans) {
		Episode subPlan = new PlainEpisode();

		DateTime prev = getDate(plan.getLegs().get(0));
		if(prev == null) {
			logger.warn("Cannot split plan. Neither start nor end time specified.");
			return;
		}
		
		for (int i = 0; i < plan.getLegs().size(); i++) {
			Attributable leg = plan.getLegs().get(i);
			Attributable act = plan.getActivities().get(i);
			
			DateTime current = getDate(leg);
			if(current == null) {
				logger.warn("Cannot split plan. Neither start nor end time specified.");
				return;
			}
			
			int currentDays = current.dayOfYear().get() + (365 * current.year().get());
			int prevDays = prev.dayOfYear().get() + (365 * prev.year().get());
			int nights = currentDays - prevDays;
			
//			if (current.dayOfYear().get() != prev.dayOfYear().get()) {
			if (nights > 0) {
				subPlan.setAttribute(MIDKeys.JOURNEY_DAYS, String.valueOf(nights + 1));

				subPlan.addActivity(((PlainSegment)act).clone());
				newPlans.add(subPlan);
				
				subPlan = new PlainEpisode();
				subPlan.addActivity(((PlainSegment)act).clone());
				subPlan.addLeg(((PlainSegment)leg).clone());
			} else {
				subPlan.setAttribute(MIDKeys.JOURNEY_DAYS, String.valueOf(nights + 1));
				
				subPlan.addActivity(((PlainSegment)act).clone());
				subPlan.addLeg(((PlainSegment)leg).clone());
			}
			
			prev = current;
		}
		
		int size = plan.getActivities().size();
		subPlan.addActivity(((PlainSegment)plan.getActivities().get(size - 1)).clone());
		newPlans.add(subPlan);
	}
	
	private DateTime getDate(Attributable leg) {
		String time = leg.getAttribute(CommonKeys.LEG_START_TIME);
		if(time == null) {
			/*
			 * This may have undesired effects in the case of over night trips.
			 */
			time = leg.getAttribute(CommonKeys.LEG_END_TIME);
		}
		
		if(time != null) {
			return formatter.parseDateTime(time);
		} else {
			return null;
		}
	}

}
