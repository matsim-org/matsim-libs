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

import java.util.Locale;

import org.joda.time.LocalDateTime;
import org.joda.time.Seconds;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.synpop.data.Element;
import playground.johannes.gsv.synPop.ProxyPlan;
import playground.johannes.gsv.synPop.ProxyPlanTask;
import playground.johannes.gsv.synPop.mid.MIDKeys;

/**
 * @author johannes
 *
 */
public class Date2TimeTask implements ProxyPlanTask {

	@Override
	public void apply(ProxyPlan plan) {
		LocalDateTime reference = null;
		
		for(Element leg : plan.getLegs()) {
			String start = leg.getAttribute(CommonKeys.LEG_START_TIME);
			if(start != null) {
				if(reference == null) {
					reference = getReference(start);
				}
				
				LocalDateTime startDate = SplitPlanTask.formatter.parseLocalDateTime(start);
				Seconds secs = Seconds.secondsBetween(reference, startDate);
				
				leg.setAttribute(CommonKeys.LEG_START_TIME, String.valueOf(secs.getSeconds()));
				if(!leg.keys().contains(MIDKeys.PERSON_MONTH)) {
					setPlanDate(startDate, plan);
				}
			}
			
			String end = leg.getAttribute(CommonKeys.LEG_END_TIME);
			if(end != null) {
				if(reference == null) {
					reference = getReference(end);
				}
				
				LocalDateTime endDate = SplitPlanTask.formatter.parseLocalDateTime(end);
				Seconds secs = Seconds.secondsBetween(reference, endDate);
				
				leg.setAttribute(CommonKeys.LEG_END_TIME, String.valueOf(secs.getSeconds()));
				
				if(!leg.keys().contains(MIDKeys.PERSON_MONTH)) {
					setPlanDate(endDate, plan);
				}
			}
		}

	}
	
	private LocalDateTime getReference(String date) {
		LocalDateTime dateTime = SplitPlanTask.formatter.parseLocalDateTime(date);
		return new LocalDateTime(dateTime.getYear(), dateTime.getMonthOfYear(), dateTime.getDayOfMonth(), 0, 0);
	}
	
	private void setPlanDate(LocalDateTime dateTime, ProxyPlan plan) {
		plan.setAttribute(MIDKeys.PERSON_MONTH, dateTime.monthOfYear().getAsShortText(Locale.US));
		plan.setAttribute(CommonKeys.DAY, dateTime.dayOfWeek().getAsShortText(Locale.US));
	}

}
