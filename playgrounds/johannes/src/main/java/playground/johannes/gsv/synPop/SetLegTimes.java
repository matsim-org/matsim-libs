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
public class SetLegTimes implements ProxyPlanTask {

	/* (non-Javadoc)
	 * @see playground.johannes.gsv.synPop.ProxyPlanTask#apply(playground.johannes.gsv.synPop.ProxyPlan)
	 */
	@Override
	public void apply(ProxyPlan plan) {
		for(int i = 0; i < plan.getLegs().size(); i++) {
			ProxyObject prev = plan.getActivities().get(i);
			ProxyObject leg = plan.getLegs().get(i);
			ProxyObject next = plan.getActivities().get(i+1);
			
			leg.setAttribute(CommonKeys.LEG_START_TIME, prev.getAttribute(CommonKeys.ACTIVITY_END_TIME));
			leg.setAttribute(CommonKeys.LEG_END_TIME, next.getAttribute(CommonKeys.ACTIVITY_START_TIME));
		}

	}

}
