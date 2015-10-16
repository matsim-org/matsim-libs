/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,       *
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

import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Segment;

/**
 * @author jillenberger
 */
public class SetActivityTimesTask implements EpisodeTask {
    @Override
    public void apply(Episode episode) {
        if (episode.getActivities().size() == 1) {
            Segment act = episode.getActivities().get(0);
            act.setAttribute(CommonKeys.ACTIVITY_START_TIME, "0");
            act.setAttribute(CommonKeys.ACTIVITY_END_TIME, "86400");
        } else {

            for (int i = 0; i < episode.getActivities().size(); i++) {
                Segment act = episode.getActivities().get(i);

                if (i == 0) act.setAttribute(CommonKeys.ACTIVITY_START_TIME, "0");
                else
                    act.setAttribute(CommonKeys.ACTIVITY_START_TIME, act.previous().getAttribute(CommonKeys.LEG_END_TIME));

                if (i == episode.getActivities().size() - 1) {
                    int startTime = Integer.parseInt(act.getAttribute(CommonKeys.ACTIVITY_START_TIME));
                    startTime = Math.max(startTime + 1, 86400);
                    act.setAttribute(CommonKeys.ACTIVITY_END_TIME, String.valueOf(startTime));
                } else {
                    act.setAttribute(CommonKeys.ACTIVITY_END_TIME, act.next().getAttribute(CommonKeys.LEG_START_TIME));
                }
            }
        }
    }
}
