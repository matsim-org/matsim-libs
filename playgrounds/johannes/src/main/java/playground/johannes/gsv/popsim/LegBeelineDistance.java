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
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Segment;

/**
 * @author johannes
 */
public class LegBeelineDistance extends LegCollector {

    private final ActivityFacilities facilities;

    public LegBeelineDistance(ActivityFacilities facilities) {
        this.facilities = facilities;
    }

    @Override
    protected Double value(Segment leg) {
        Segment prev = leg.previous();
        Segment next = leg.next();

        String prevFacId = prev.getAttribute(CommonKeys.ACTIVITY_FACILITY);
        String nextFacId = next.getAttribute(CommonKeys.ACTIVITY_FACILITY);

        ActivityFacility prevFac = facilities.getFacilities().get(Id.create(prevFacId, ActivityFacility.class));
        ActivityFacility nextFac = facilities.getFacilities().get(Id.create(nextFacId, ActivityFacility.class));

        if(prevFac != null && nextFac != null) {
            double dx = prevFac.getCoord().getX() - nextFac.getCoord().getX();
            double dy = prevFac.getCoord().getY() - nextFac.getCoord().getY();

            return Math.sqrt(dx * dx + dy *dy);
        } else return null;
    }
}
