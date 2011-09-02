/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityFacilityModAdaptor.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.coopsim.mental.planmod;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.router.NetworkLegRouter;

import playground.johannes.coopsim.mental.choice.ActivityFacilitySelector;
import playground.johannes.coopsim.mental.choice.PlanIndexSelector;

/**
 * @author illenberger
 *
 */
public class ActivityFacilityModAdaptor implements Choice2ModAdaptor {

	private final ActivityFacilityMod mod;
	
	public ActivityFacilityModAdaptor(ActivityFacilities facilities, NetworkLegRouter router) {
		mod = new ActivityFacilityMod(facilities, router);
	}
	
	@Override
	public PlanModifier convert(Map<String, Object> choices) {
		int index = (Integer) choices.get(PlanIndexSelector.KEY);
		Id facilityId = (Id) choices.get(ActivityFacilitySelector.KEY);
		
		mod.setPlanIndex(index);
		mod.setFacilityId(facilityId);
		
		return mod;
	}

}
