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

import playground.johannes.gsv.synPop.ActivityType;
import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyPlanTask;
import playground.johannes.synpop.data.Element;
import playground.johannes.synpop.data.Episode;

/**
 * @author johannes
 */
public class SetLegPurposes implements ProxyPlanTask {


    @Override
    public void apply(Episode episode) {
        for(int i = 0; i < episode.getLegs().size(); i++) {
            Element leg = episode.getLegs().get(i);
            String nextType = episode.getActivities().get(i + 1).getAttribute(CommonKeys.ACTIVITY_TYPE);
            /*
            If the next activity is a home activity, use the type of the previous activity as purpose, otherwise use
            the next activity type.
             */
            if(ActivityType.HOME.equalsIgnoreCase(nextType)) {
                String prevType = episode.getActivities().get(i).getAttribute(CommonKeys.ACTIVITY_TYPE);
                leg.setAttribute(CommonKeys.LEG_PURPOSE, prevType);
            } else {
                leg.setAttribute(CommonKeys.LEG_PURPOSE, nextType);
            }
        }
    }
}
