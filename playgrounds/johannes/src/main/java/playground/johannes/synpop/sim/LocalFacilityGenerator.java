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
package playground.johannes.synpop.sim;

import org.matsim.api.core.v01.Coord;
import org.matsim.facilities.ActivityFacility;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.gis.FacilityData;
import playground.johannes.synpop.sim.data.CachedElement;
import playground.johannes.synpop.sim.data.CachedSegment;
import playground.johannes.synpop.sim.data.Converters;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author jillenberger
 */
public class LocalFacilityGenerator implements ValueGenerator {

    private final FacilityData facilityData;

    private final List<String> blacklist;

    private  Object facilityDataKey;

    private final Random random;

    public LocalFacilityGenerator(FacilityData data, Random random) {
        this.facilityData = data;
        this.blacklist = new ArrayList<>();
        this.random = random;
    }

    public void addToBlacklist(String type) {
        blacklist.add(type);
    }

    @Override
    public Object newValue(CachedElement element) {
        CachedSegment act = (CachedSegment) element;

        /*
        Won't work if activity types change.
         */
        String type = act.getAttribute(CommonKeys.ACTIVITY_TYPE);
        boolean ignore = false;
            if (type != null) {
                if (blacklist.contains(type)) ignore = true;
            }
        if (!ignore) {
            CachedSegment leg = (CachedSegment) act.previous();
            CachedSegment prev = null;
            if(leg != null) prev = (CachedSegment) leg.previous();

            if(prev != null) {
                if(facilityDataKey == null) facilityDataKey = Converters.getObjectKey(CommonKeys.ACTIVITY_FACILITY);
                ActivityFacility prefFac = (ActivityFacility) prev.getData(facilityDataKey);
                for(int i = 0; i < 100; i++) {
                    ActivityFacility newFac = facilityData.randomFacility(type);
                    double d = distance(prefFac, newFac);
                    d = d/1000.0;
                    double p = Math.pow(d, -0.5);
                    if(p >= random.nextDouble()) {
                        return newFac;
                    }
                }
                /*
                if no suitable facility is found, draw a random one
                 */
                return facilityData.randomFacility(type);
            } else {
                return facilityData.randomFacility(type);
            }
        } else {
            return null;
        }
    }

    private double distance(ActivityFacility f1, ActivityFacility f2) {
        Coord c1 = f1.getCoord();
        Coord c2 = f2.getCoord();
        double dx = c1.getX() - c2.getX();
        double dy = c1.getY() - c2.getY();

        return Math.sqrt(dx * dx + dy * dy);
    }
}
