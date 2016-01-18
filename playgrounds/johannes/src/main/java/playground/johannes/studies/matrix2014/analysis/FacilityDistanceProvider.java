/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.johannes.studies.matrix2014.analysis;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import playground.johannes.synpop.analysis.ValueProvider;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Segment;

/**
 * @author johannes
 */
public class FacilityDistanceProvider implements ValueProvider<Double, Segment> {

    private final ActivityFacilities facilities;

    public FacilityDistanceProvider(ActivityFacilities facilities) {
        this.facilities = facilities;
    }

    @Override
    public Double get(Segment attributable) {
        Segment prev = attributable.previous();
        Segment next = attributable.next();

        double d = distance(prev, next);
        return d;
    }

    private double distance(Segment from, Segment to) {
        ActivityFacility fac1 = getFacility(from);
        ActivityFacility fac2 = getFacility(to);

        Coord c1 = fac1.getCoord();
        Coord c2 = fac2.getCoord();

        double dx = c1.getX() - c2.getX();
        double dy = c1.getY() - c2.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    private ActivityFacility getFacility(Segment act) {
        String facilityId = act.getAttribute(CommonKeys.ACTIVITY_FACILITY);
        Id<ActivityFacility> facilityObjId = Id.create(facilityId, ActivityFacility.class);
        ActivityFacility fac = facilities.getFacilities().get(facilityObjId);
        return  fac;
    }
}
