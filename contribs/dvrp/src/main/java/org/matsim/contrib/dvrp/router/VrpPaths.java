/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.router;

import java.util.Comparator;


public class VrpPaths
{
    public static final Comparator<VrpPathWithTravelData> TRAVEL_TIME_COMPARATOR = new Comparator<VrpPathWithTravelData>() {
        public int compare(VrpPathWithTravelData p1, VrpPathWithTravelData p2)
        {
            return Double.compare(p1.getTravelTime(), p2.getTravelTime());
        }
    };

    public static final Comparator<VrpPathWithTravelData> ARRIVAL_TIME_COMPARATOR = new Comparator<VrpPathWithTravelData>() {
        public int compare(VrpPathWithTravelData p1, VrpPathWithTravelData p2)
        {
            return Double.compare(p1.getArrivalTime(), p2.getArrivalTime());
        }
    };

    public static final Comparator<VrpPathWithTravelData> DEPARTURE_TIME_COMPARATOR = new Comparator<VrpPathWithTravelData>() {
        public int compare(VrpPathWithTravelData p1, VrpPathWithTravelData p2)
        {
            return Double.compare(p1.getDepartureTime(), p2.getDepartureTime());
        }
    };

    public static final Comparator<VrpPathWithTravelData> TRAVEL_COST_COMPARATOR = new Comparator<VrpPathWithTravelData>() {
        public int compare(VrpPathWithTravelData p1, VrpPathWithTravelData p2)
        {
            return Double.compare(p1.getTravelCost(), p2.getTravelCost());
        }
    };
}
