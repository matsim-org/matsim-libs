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

import org.matsim.api.core.v01.Id;
import org.matsim.facilities.ActivityFacility;
import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyPlanTask;
import playground.johannes.gsv.synPop.data.DataPool;
import playground.johannes.gsv.synPop.data.FacilityData;
import playground.johannes.gsv.synPop.data.FacilityDataLoader;
import playground.johannes.synpop.data.Element;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.PlainElement;

public class InitActivitLocations implements ProxyPlanTask {

	private final FacilityData data;

	public InitActivitLocations(DataPool dataPool) {
		this.data = (FacilityData) dataPool.get(FacilityDataLoader.KEY);
	}

	@Override
	public void apply(Episode plan) {
		for (Element act : plan.getActivities()) {
			String id = act.getAttribute(CommonKeys.ACTIVITY_FACILITY);
			if (id != null) {
				Id<ActivityFacility> idObj = Id.create(id, ActivityFacility.class);
				ActivityFacility f = data.getAll().getFacilities().get(idObj);
				((PlainElement)act).setUserData(ActivityLocationMutator.USER_DATA_KEY, f);
			} else {
				String type = act.getAttribute(CommonKeys.ACTIVITY_TYPE);
				ActivityFacility f = data.randomFacility(type);
				((PlainElement)act).setUserData(ActivityLocationMutator.USER_DATA_KEY, f);

			}
		}

	}

}
