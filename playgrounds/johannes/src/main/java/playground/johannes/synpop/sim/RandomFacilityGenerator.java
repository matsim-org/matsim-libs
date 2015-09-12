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

import playground.johannes.gsv.synPop.data.FacilityData;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.sim.data.CachedElement;
import playground.johannes.synpop.sim.data.CachedSegment;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jillenberger
 */
public class RandomFacilityGenerator implements ValueGenerator {

    private static final Object IGNORE_KEY = new Object();

    private final FacilityData facilityData;

    private final List<String> blacklist;

    public RandomFacilityGenerator(FacilityData data) {
        this.facilityData = data;
        this.blacklist = new ArrayList<>();
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
        Boolean ignore = (Boolean) act.getData(IGNORE_KEY);
        if (ignore == null) {
            ignore = false;

            if (type != null) {
                if (blacklist.contains(type)) ignore = true;
            }

            act.setData(IGNORE_KEY, ignore);
        }

        if (!ignore) {
            return facilityData.randomFacility(type);
        } else {
            return null;
        }
    }
}
