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

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.synpop.data.Element;
import playground.johannes.gsv.synPop.ProxyPlan;
import playground.johannes.gsv.synPop.ProxyPlanTask;

/**
 * @author johannes
 *
 */
public class SetActivityTypes implements ProxyPlanTask {

	/* (non-Javadoc)
	 * @see playground.johannes.gsv.synPop.ProxyPlanTask#apply(playground.johannes.gsv.synPop.ProxyPlan)
	 */
	@Override
	public void apply(ProxyPlan plan) {
		for(Element act : plan.getActivities()) {
			if(InvermoKeys.HOME.equals(act.getAttribute(InvermoKeys.LOCATION))) {
				act.setAttribute(CommonKeys.ACTIVITY_TYPE, InvermoKeys.HOME);
			}
		}
		
		for(int i = 0; i < plan.getLegs().size(); i++) {
			Element leg = plan.getLegs().get(i);
			Element act = plan.getActivities().get(i + 1);
			
			if(!InvermoKeys.HOME.equals(act.getAttribute(InvermoKeys.LOCATION))) {
				act.setAttribute(CommonKeys.ACTIVITY_TYPE, leg.getAttribute(CommonKeys.LEG_PURPOSE));
			}
		}

	}

}
