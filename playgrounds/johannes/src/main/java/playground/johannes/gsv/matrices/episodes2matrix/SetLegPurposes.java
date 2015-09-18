/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.matrices.episodes2matrix;

import playground.johannes.synpop.data.ActivityTypes;
import playground.johannes.synpop.data.Attributable;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.processing.EpisodeTask;

/**
 * @author johannes
 */
public class SetLegPurposes implements EpisodeTask {


    @Override
    public void apply(Episode episode) {
        for(int i = 0; i < episode.getLegs().size(); i++) {
            Attributable leg = episode.getLegs().get(i);
            String nextType = episode.getActivities().get(i + 1).getAttribute(CommonKeys.ACTIVITY_TYPE);
            /*
            If the next activity is a home activity, use the type of the previous activity as purpose, otherwise use
            the next activity type.
             */
            if(ActivityTypes.HOME.equalsIgnoreCase(nextType)) {
                String prevType = episode.getActivities().get(i).getAttribute(CommonKeys.ACTIVITY_TYPE);
                leg.setAttribute(CommonKeys.LEG_PURPOSE, prevType);
            } else {
                leg.setAttribute(CommonKeys.LEG_PURPOSE, nextType);
            }
        }
    }
}
