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
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.processing.EpisodeTask;
import playground.johannes.synpop.data.Attributable;
import playground.johannes.synpop.data.CommonValues;
import playground.johannes.synpop.data.Episode;

/**
 * @author johannes
 */
public class InfereWeCommuter implements EpisodeTask {

    public static final String WECOMMUTER = "wecommuter";

    private final double threshold;

    public InfereWeCommuter(double threshold) {
        this.threshold = threshold;
    }

    @Override
    public void apply(Episode plan) {
        String day = plan.getPerson().getAttribute(CommonKeys.DAY);

        if(day.equalsIgnoreCase(CommonValues.THURSDAY) || day.equalsIgnoreCase(CommonValues.FRIDAY) || day
                .equalsIgnoreCase(CommonValues.SATURDAY) || day.equalsIgnoreCase(CommonValues.SUNDAY)) {

            for(int i = 0; i < plan.getLegs().size(); i++) {
                Attributable leg = plan.getLegs().get(i);
                Attributable prev = plan.getActivities().get(i);
                Attributable next = plan.getActivities().get(i + 1);

                String distanceVal = leg.getAttribute(CommonKeys.LEG_GEO_DISTANCE);
                if(distanceVal != null) {
                    double d = Double.parseDouble(distanceVal);
                    if(d > threshold) {
                        String prevType = prev.getAttribute(CommonKeys.ACTIVITY_TYPE);
                        String nextType = next.getAttribute(CommonKeys.ACTIVITY_TYPE);

                        if(ActivityType.WORK.equalsIgnoreCase(prevType) & ActivityType.HOME.equalsIgnoreCase
                                (nextType)) {
                            leg.setAttribute(CommonKeys.LEG_PURPOSE, WECOMMUTER);
                        } else if (ActivityType.WORK.equalsIgnoreCase(nextType) & ActivityType.HOME.equalsIgnoreCase
                                (prevType)) {
                            leg.setAttribute(CommonKeys.LEG_PURPOSE, WECOMMUTER);
                        }
                    }
                }
            }
        }
    }
}
