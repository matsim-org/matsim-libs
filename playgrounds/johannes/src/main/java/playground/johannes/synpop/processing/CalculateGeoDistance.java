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

package playground.johannes.synpop.processing;

import org.matsim.api.core.v01.Id;
import org.matsim.facilities.ActivityFacility;
import playground.johannes.gsv.synPop.data.FacilityData;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Segment;

/**
 * @author johannes
 */
public class CalculateGeoDistance implements EpisodeTask {

    private final FacilityData data;

    public CalculateGeoDistance(FacilityData data) {
       this.data = data;
    }

    @Override
    public void apply(Episode episode) {
        for(int i = 0; i < episode.getLegs().size(); i++) {
            Segment from = episode.getActivities().get(i);
            Segment to = episode.getActivities().get(i + 1);

            Id<ActivityFacility> idFrom = Id.create(from.getAttribute(CommonKeys.ACTIVITY_FACILITY), ActivityFacility
                    .class);
            Id<ActivityFacility> idTo = Id.create(to.getAttribute(CommonKeys.ACTIVITY_FACILITY), ActivityFacility
                    .class);

            ActivityFacility facFrom = data.getAll().getFacilities().get(idFrom);
            ActivityFacility facTo = data.getAll().getFacilities().get(idTo);

            double dx = facFrom.getCoord().getX() - facTo.getCoord().getX();
            double dy = facFrom.getCoord().getY() - facTo.getCoord().getY();
            double d = Math.sqrt(dx*dx + dy*dy);

            Segment leg = episode.getLegs().get(i);
            leg.setAttribute(CommonKeys.LEG_GEO_DISTANCE, String.valueOf(d));
        }
    }
}
