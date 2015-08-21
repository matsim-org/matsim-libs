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

package playground.johannes.gsv.qlik;

import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.source.mid2008.processing.EpisodeTask;
import playground.johannes.synpop.data.Episode;

/**
 * @author johannes
 */
public class CopyActTypeAttributes implements EpisodeTask {

    public static final String PREV_ACT_TYPE = "prevType";

    public static final String NEXT_ACT_TYPE = "nextType";

    @Override
    public void apply(Episode plan) {
        for(int i = 0; i < plan.getLegs().size(); i++) {
            String prev = plan.getActivities().get(i).getAttribute(CommonKeys.ACTIVITY_TYPE);
            String next = plan.getActivities().get(i + 1).getAttribute(CommonKeys.ACTIVITY_TYPE);

            if(prev != null && next != null) {
                plan.getLegs().get(i).setAttribute(PREV_ACT_TYPE, prev);
                plan.getLegs().get(i).setAttribute(NEXT_ACT_TYPE, next);
            }
        }
    }
}
