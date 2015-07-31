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
public class ValidateActTimesTask implements ProxyPlanTask {

	/* (non-Javadoc)
	 * @see playground.johannes.gsv.synPop.ProxyPlanTask#apply(playground.johannes.gsv.synPop.ProxyPlan)
	 */
	@Override
	public void apply(ProxyPlan plan) {
		int prev = 0;
		for(int i = 0; i < plan.getActivities().size(); i++) {
			Element act = plan.getActivities().get(i);
			int start = Integer.parseInt(act.getAttribute(CommonKeys.ACTIVITY_START_TIME));
			int end = Integer.parseInt(act.getAttribute(CommonKeys.ACTIVITY_END_TIME));
			
			start = Math.max(start, prev);

			act.setAttribute(CommonKeys.ACTIVITY_START_TIME, String.valueOf(start));
			
			end = Math.max(start, end);
			
			act.setAttribute(CommonKeys.ACTIVITY_END_TIME, String.valueOf(end));
			
			prev = end;
		}

	}

}
