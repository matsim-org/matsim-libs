/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.matrices;

import playground.johannes.gsv.synPop.ActivityType;
import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyObject;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.ProxyPlan;

/**
 * @author johannes
 * 
 */
public class WeCommuterPredicate implements Predicate {

	private final double threshold;

	public WeCommuterPredicate(double threshold) {
		this.threshold = threshold;
	}
	
	@Override
	public boolean test(ProxyPerson person, ProxyObject leg, ProxyObject prev, ProxyObject next) {
		boolean result = false;

		String day = person.getAttribute(CommonKeys.DAY);
		if (day != null) {
			if (day.equalsIgnoreCase(CommonKeys.THURSDAY) || day.equalsIgnoreCase(CommonKeys.FRIDAY) || day.equalsIgnoreCase(CommonKeys.SATURDAY)
					|| day.equalsIgnoreCase(CommonKeys.SUNDAY)) {
				
				ProxyPlan plan = person.getPlans().get(0);
				ProxyObject weLeg = null;
				
				int cnt = 0;
				for (int i = 0; i < plan.getLegs().size(); i++) {
					ProxyObject leg2 = plan.getLegs().get(i);
					ProxyObject prev2 = plan.getActivities().get(i);
					ProxyObject next2 = plan.getActivities().get(i + 1);

					String val = leg2.getAttribute(CommonKeys.LEG_GEO_DISTANCE);
					if (val != null) {
						double d = Double.parseDouble(val);
						if (d > threshold) {
							if (ActivityType.WORK.equalsIgnoreCase(prev2.getAttribute(CommonKeys.ACTIVITY_TYPE))
									|| ActivityType.WORK.equalsIgnoreCase(next2.getAttribute(CommonKeys.ACTIVITY_TYPE))) {
								cnt++;
								weLeg = leg2;
							}
						}
					}
				}

				if (cnt == 1) {
					if(weLeg == leg) {
						return true;
					}
				}

			}
		}

		return result;
	}

}
