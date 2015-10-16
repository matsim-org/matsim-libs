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

package playground.johannes.synpop.processing;


import playground.johannes.synpop.data.ActivityTypes;
import playground.johannes.synpop.data.Attributable;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Episode;

/**
 * @author johannes
 *
 */
public class SetActivityTypeTask implements EpisodeTask {

	@Override
	public void apply(Episode episode) {
		if(episode.getLegs().isEmpty()) {
			episode.getActivities().get(0).setAttribute(CommonKeys.ACTIVITY_TYPE, ActivityTypes.HOME);
		}
		
		for(int i = 0; i < episode.getLegs().size(); i++) {
			Attributable leg = episode.getLegs().get(i);
			Attributable act = episode.getActivities().get(i + 1);
			
			act.setAttribute(CommonKeys.ACTIVITY_TYPE, leg.getAttribute(CommonKeys.LEG_PURPOSE));
		}

	}

}
