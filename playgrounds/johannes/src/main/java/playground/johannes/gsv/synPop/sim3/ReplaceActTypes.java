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

import playground.johannes.synpop.data.ActivityTypes;
import playground.johannes.synpop.data.Attributable;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.processing.EpisodeTask;

import java.util.HashMap;
import java.util.Map;

/**
 * @author johannes
 * 
 */
public class ReplaceActTypes implements EpisodeTask {

	public static final String ORIGINAL_TYPE = "origType";

	private static Map<String, String> typeMapping;

	public Map<String, String> getTypeMapping() {
		if (typeMapping == null) {
			typeMapping = new HashMap<String, String>();
			typeMapping.put("vacations_short", ActivityTypes.LEISURE);
			typeMapping.put("vacations_long", ActivityTypes.LEISURE);
			typeMapping.put("visit", ActivityTypes.LEISURE);
			typeMapping.put("culture", ActivityTypes.LEISURE);
			typeMapping.put("gastro", ActivityTypes.LEISURE);
			typeMapping.put(ActivityTypes.BUSINESS, ActivityTypes.WORK);
			typeMapping.put("private", ActivityTypes.MISC);
			typeMapping.put("pickdrop", ActivityTypes.MISC);
			typeMapping.put("sport", ActivityTypes.LEISURE);
			typeMapping.put("wecommuter", ActivityTypes.WORK);
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
