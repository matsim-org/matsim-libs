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

package playground.johannes.gsv.synPop.sim3;

import org.matsim.core.api.experimental.facilities.ActivityFacility;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyObject;
import playground.johannes.gsv.synPop.ProxyPlan;
import playground.johannes.gsv.synPop.ProxyPlanTask;
import playground.johannes.gsv.synPop.data.DataPool;
import playground.johannes.gsv.synPop.data.FacilityData;
import playground.johannes.gsv.synPop.data.FacilityDataLoader;

public class InitActivitLocations implements ProxyPlanTask {

	private final FacilityData data;

	private final String blacklist;
	
	public InitActivitLocations(DataPool dataPool, String blacklist) {
		this.data = (FacilityData) dataPool.get(FacilityDataLoader.KEY);
		this.blacklist = blacklist;
	}

	@Override
	public void apply(ProxyPlan plan) {
		for(ProxyObject act : plan.getActivities()) {
			String type = act.getAttribute(CommonKeys.ACTIVITY_TYPE);
			if(blacklist == null || !blacklist.equalsIgnoreCase(type)) {
				ActivityFacility f = data.randomFacility(type);
				act.setUserData(ActivityLocationMutator.USER_DATA_KEY, f);
			}
		}

	}

}
