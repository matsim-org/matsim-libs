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

import playground.johannes.gsv.synPop.ActivityType;
import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyPlanTask;
import playground.johannes.synpop.data.Attributable;
import playground.johannes.synpop.data.Episode;

import java.util.HashMap;
import java.util.Map;

/**
 * @author johannes
 * 
 */
public class ReplaceActTypes implements ProxyPlanTask {

	public static final String ORIGINAL_TYPE = "origType";

	private static Map<String, String> typeMapping;

	public Map<String, String> getTypeMapping() {
		if (typeMapping == null) {
			typeMapping = new HashMap<String, String>();
			typeMapping.put("vacations_short", ActivityType.LEISURE);
			typeMapping.put("vacations_long", ActivityType.LEISURE);
			typeMapping.put("visit", ActivityType.LEISURE);
			typeMapping.put("culture", ActivityType.LEISURE);
			typeMapping.put("gastro", ActivityType.LEISURE);
			typeMapping.put(ActivityType.BUISINESS, ActivityType.WORK);
			typeMapping.put("private", ActivityType.MISC);
			typeMapping.put("pickdrop", ActivityType.MISC);
			typeMapping.put("sport", ActivityType.LEISURE);
			typeMapping.put("wecommuter", ActivityType.WORK);
		}

		return typeMapping;
	}

	@Override
	public void apply(Episode plan) {
		for (Attributable act : plan.getActivities()) {
			String origType = act.getAttribute(ORIGINAL_TYPE);
			if (origType == null) {
				String type = act.getAttribute(CommonKeys.ACTIVITY_TYPE);
				act.setAttribute(ORIGINAL_TYPE, type);
				String newType = getTypeMapping().get(type);
				if (newType != null) {
					act.setAttribute(CommonKeys.ACTIVITY_TYPE, newType);
				}
			}
		}

	}

}
