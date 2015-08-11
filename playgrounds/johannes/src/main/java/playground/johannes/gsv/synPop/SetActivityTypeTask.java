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
import playground.johannes.synpop.data.Episode;

/**
 * @author johannes
 *
 */
public class SetActivityTypeTask implements ProxyPlanTask {

	/* (non-Javadoc)
	 * @see playground.johannes.gsv.synPop.ProxyPlanTask#apply(playground.johannes.synpop.data.PlainEpisode)
	 */
	@Override
	public void apply(Episode plan) {
		if(plan.getLegs().isEmpty()) {
			plan.getActivities().get(0).setAttribute(CommonKeys.ACTIVITY_TYPE, "home");
		}
		
		for(int i = 0; i < plan.getLegs().size(); i++) {
			Attributable leg = plan.getLegs().get(i);
			Attributable act = plan.getActivities().get(i + 1);
			
			act.setAttribute(CommonKeys.ACTIVITY_TYPE, leg.getAttribute(CommonKeys.LEG_PURPOSE));
		}

	}

}
