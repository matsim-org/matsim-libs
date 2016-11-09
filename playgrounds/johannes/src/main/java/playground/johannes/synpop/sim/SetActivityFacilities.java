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

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.common.util.XORShiftRandom;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.ActivityFacility;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Segment;
import playground.johannes.synpop.gis.FacilityData;
import playground.johannes.synpop.processing.EpisodeTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author johannes
 */
public class SetActivityFacilities implements EpisodeTask {

    private final FacilityData data;

    private final Random random;

    private final double rangeFactor;

    private final double fallbackDistance = 10000;

    public SetActivityFacilities(FacilityData data) {
        this(data, 0.1, new XORShiftRandom());
    }

    public SetActivityFacilities(FacilityData data, double rangeFactor, Random random) {
        this.data = data;
        this.random = random;
        this.rangeFactor = rangeFactor;
    }

    @Override
    public void apply(Episode episode) {
        for(Segment act : episode.getActivities()) {
            if(act.getAttribute(CommonKeys.ACTIVITY_FACILITY) == null) {
                String type = act.getAttribute(CommonKeys.ACTIVITY_TYPE);
                ActivityFacility facility = null;
                Segment toLeg = act.previous();
                /*
                get random facility in distance range
                 */
                if(toLeg != null) {
                    Segment prevAct = toLeg.previous();
                    Id<ActivityFacility> originId = Id.create(
                            prevAct.getAttribute(CommonKeys.ACTIVITY_FACILITY),
                            ActivityFacility.class);
                    ActivityFacility origin = data.getAll().getFacilities().get(originId);

                    String distance = toLeg.getAttribute(CommonKeys.LEG_GEO_DISTANCE);
                    double d = fallbackDistance;
                    if(distance != null) d = Double.parseDouble(distance);

                    QuadTree<ActivityFacility> quadTree = data.getQuadTree(type);

                    double range = d * rangeFactor;
                    List<ActivityFacility> facilityList = new ArrayList<>(quadTree.getRing(
                            origin.getCoord().getX(),
                            origin.getCoord().getY(),
                            d - range,
                            d + range));

                    if(facilityList.isEmpty())
                        facility = quadTree.getClosest(origin.getCoord().getX(), origin.getCoord().getY());
                    else
                        facility = facilityList.get(random.nextInt(facilityList.size()));

                }
                /*
                fallback to random facility
                 */
                if(facility == null) {
                    facility = data.randomFacility(type);
                }

                act.setAttribute(CommonKeys.ACTIVITY_FACILITY, facility.getId().toString());
            }
        }
    }
}
