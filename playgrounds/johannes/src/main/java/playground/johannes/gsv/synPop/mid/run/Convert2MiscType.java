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

package playground.johannes.gsv.synPop.mid.run;

import playground.johannes.synpop.data.ActivityTypes;
import playground.johannes.synpop.data.Attributable;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.processing.EpisodeTask;

/**
 * @author johannes
 *
 */
public class Convert2MiscType implements EpisodeTask {

	/* (non-Javadoc)
	 * @see playground.johannes.synpop.processing.EpisodeTask#apply(playground.johannes.synpop.data.PlainEpisode)
	 */
	@Override
	public void apply(Episode plan) {
		for(Attributable act : plan.getActivities()) {
			String type = act.getAttribute(CommonKeys.ACTIVITY_TYPE);
			if("pickdrop".equalsIgnoreCase(type)) {
				act.setAttribute(CommonKeys.ACTIVITY_TYPE, ActivityTypes.MISC);
			} else if("private".equalsIgnoreCase(type)) {
				act.setAttribute(CommonKeys.ACTIVITY_TYPE, ActivityTypes.MISC);
			} else if("intown".equalsIgnoreCase(type)) {
				act.setAttribute(CommonKeys.ACTIVITY_TYPE, ActivityTypes.MISC);
			} else if("outoftown".equalsIgnoreCase(type)) {
				act.setAttribute(CommonKeys.ACTIVITY_TYPE, ActivityTypes.MISC);
			} else if("unknown".equalsIgnoreCase(type)) {
				act.setAttribute(CommonKeys.ACTIVITY_TYPE, ActivityTypes.MISC);
			}
			
		}

	}

}
