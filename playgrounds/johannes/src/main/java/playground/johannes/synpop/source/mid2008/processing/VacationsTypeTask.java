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

package playground.johannes.synpop.source.mid2008.processing;

import playground.johannes.synpop.data.ActivityTypes;
import playground.johannes.synpop.data.Attributable;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.processing.EpisodeTask;
import playground.johannes.synpop.source.mid2008.MiDKeys;

/**
 * @author johannes
 * 
 */
public class VacationsTypeTask implements EpisodeTask {

	@Override
	public void apply(Episode plan) {
		for (Attributable act : plan.getActivities()) {
			if (act.getAttribute(CommonKeys.ACTIVITY_TYPE).equalsIgnoreCase(ActivityTypes.LEISURE)) {
				String val = plan.getAttribute(MiDKeys.JOURNEY_DAYS);
				int days = 0;

				if (val != null)
					days = Integer.parseInt(val);
				
				if (days > 4) {
					act.setAttribute(CommonKeys.ACTIVITY_TYPE, ActivityTypes.VACATIONS_LONG);
				} else  if(days > 1 && days <= 4) {
					act.setAttribute(CommonKeys.ACTIVITY_TYPE, ActivityTypes.VACATIONS_SHORT);
				}

			}
		}

	}

}
