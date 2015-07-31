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

package playground.johannes.gsv.synPop.mid;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.synpop.data.Element;
import playground.johannes.gsv.synPop.ProxyPlan;
import playground.johannes.gsv.synPop.ProxyPlanTask;

/**
 * @author johannes
 * 
 */
public class InfereVacationsType implements ProxyPlanTask {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * playground.johannes.gsv.synPop.ProxyPlanTask#apply(playground.johannes
	 * .gsv.synPop.ProxyPlan)
	 */
	@Override
	public void apply(ProxyPlan plan) {
		for (Element act : plan.getActivities()) {
			if (act.getAttribute(CommonKeys.ACTIVITY_TYPE).equalsIgnoreCase("vacations")) {
				String val = plan.getAttribute("journeydays");
				int days = 0;
				if (val != null)
					days = Integer.parseInt(val);
				
				if (days > 4) {
					act.setAttribute(CommonKeys.ACTIVITY_TYPE, "vacations_long");
				} else {
					act.setAttribute(CommonKeys.ACTIVITY_TYPE, "vacations_short");
				}

			}
		}

	}

}
