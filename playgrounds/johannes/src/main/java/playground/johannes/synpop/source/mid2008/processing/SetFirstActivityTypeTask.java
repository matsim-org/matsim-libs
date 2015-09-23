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
 */
public class SetFirstActivityTypeTask implements EpisodeTask {

    @Override
    public void apply(Episode episode) {
        if (episode.getLegs().size() > 0) {
            Attributable firstLeg = episode.getLegs().get(0);
            Attributable firstAct = episode.getActivities().get(0);

            String origin = firstLeg.getAttribute(MiDKeys.LEG_ORIGIN);
            if (ActivityTypes.HOME.equals(origin) || ActivityTypes.WORK.equals(origin)) {
                firstAct.setAttribute(CommonKeys.ACTIVITY_TYPE, origin);
            }

        }
    }

}
