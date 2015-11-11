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

package playground.johannes.gsv.popsim;

import org.matsim.api.core.v01.Id;
import org.matsim.facilities.ActivityFacility;
import playground.johannes.gsv.popsim.analysis.Predicate;
import playground.johannes.gsv.zones.KeyMatrix;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.Segment;
import playground.johannes.synpop.gis.FacilityData;

import java.util.Collection;

/**
 * @author johannes
 */
public class MatrixBuilder {

    public static KeyMatrix build(Collection<? extends Person> persons, Predicate<Segment> predicate, FacilityData
            fData) {
        KeyMatrix m = new KeyMatrix();

        for (Person person : persons) {
            for (Episode episode : person.getEpisodes()) {
                for (int i = 0; i < episode.getLegs().size(); i++) {
                    Segment leg = episode.getLegs().get(i);
                    if (predicate.test(leg)) {
                        Segment prev = episode.getActivities().get(i);
                        Segment next = episode.getActivities().get(i + 1);

                        Id<ActivityFacility> fromId = Id.create(prev.getAttribute(CommonKeys.ACTIVITY_FACILITY),
                                ActivityFacility.class);
                        ActivityFacility fromFac = fData.getAll().getFacilities().get(fromId);
                        String origin = fData.getAttribute(fromFac, "zoneId");

                        Id<ActivityFacility> toId = Id.create(next.getAttribute(CommonKeys.ACTIVITY_FACILITY),
                                ActivityFacility.class);
                        ActivityFacility toFac = fData.getAll().getFacilities().get(toId);
                        String dest = fData.getAttribute(toFac, "zoneId");

                        if(origin != null && dest != null) {
                            m.add(origin, dest, 1);

//                            String touched = leg.getAttribute(DBG_TOUCHED);
//                            int cnt = 0;
//                            if(touched != null) cnt = Integer.parseInt(touched);
//                            leg.setAttribute(DBG_TOUCHED, String.valueOf(cnt + 1));
                        }
                    }
                }
            }
        }
        return m;
    }
}
