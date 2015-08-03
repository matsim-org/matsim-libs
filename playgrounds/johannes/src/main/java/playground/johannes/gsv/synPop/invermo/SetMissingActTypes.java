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

import playground.johannes.gsv.synPop.ActivityType;
import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyPlanTask;
import playground.johannes.synpop.data.Element;
import playground.johannes.synpop.data.Episode;

/**
 * @author johannes
 *
 */
public class SetMissingActTypes implements ProxyPlanTask {

	@Override
	public void apply(Episode plan) {
		for(Element act : plan.getActivities()) {
			if(act.getAttribute(CommonKeys.ACTIVITY_TYPE) == null) {
				act.setAttribute(CommonKeys.ACTIVITY_TYPE, ActivityType.LEISURE);
			}
		}
		
	}

}
