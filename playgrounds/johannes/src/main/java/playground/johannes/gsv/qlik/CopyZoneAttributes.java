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

import playground.johannes.gsv.matrices.episodes2matrix.SetZones;
import playground.johannes.synpop.source.mid2008.processing.EpisodeTask;
import playground.johannes.synpop.data.Episode;

/**
 * @author johannes
 */
public class CopyZoneAttributes implements EpisodeTask {

    public static final String FROM_ZONE_KEY = "fromZone";

    public static final String TO_ZONE_KEY = "toZone";

    @Override
    public void apply(Episode plan) {
        for(int i = 0; i < plan.getLegs().size(); i++) {
            String from = plan.getActivities().get(i).getAttribute(SetZones.ZONE_KEY);
            String to = plan.getActivities().get(i + 1).getAttribute(SetZones.ZONE_KEY);

            if(from != null && to != null) {
                plan.getLegs().get(i).setAttribute(FROM_ZONE_KEY, from);
                plan.getLegs().get(i).setAttribute(TO_ZONE_KEY, to);
            }
        }
    }
}
