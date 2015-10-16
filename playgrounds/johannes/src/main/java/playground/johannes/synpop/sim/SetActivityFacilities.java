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

package playground.johannes.synpop.sim;

import org.matsim.facilities.ActivityFacility;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Segment;
import playground.johannes.synpop.gis.FacilityData;
import playground.johannes.synpop.processing.EpisodeTask;

/**
 * @author johannes
 */
public class SetActivityFacilities implements EpisodeTask {

    private final FacilityData data;

    public SetActivityFacilities(FacilityData data) {
        this.data = data;
    }

    @Override
    public void apply(Episode episode) {
        for(Segment act : episode.getActivities()) {
            if(act.getAttribute(CommonKeys.ACTIVITY_FACILITY) == null) {
                String type = act.getAttribute(CommonKeys.ACTIVITY_TYPE);
//                if(type == null) type = ActivityTypes.MISC;//FIXME
                ActivityFacility f = data.randomFacility(type);
                act.setAttribute(CommonKeys.ACTIVITY_FACILITY, f.getId().toString());
            }
        }
    }
}
