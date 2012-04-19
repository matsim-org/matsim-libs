/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.michalm.vrp.data.network.shortestpath;

import org.matsim.api.core.v01.Id;


public interface ShortestPath
{
    // optimization
    public static final SPEntry ZERO_PATH_ENTRY = new SPEntry(0, 0, new Id[0]);

    // include toLink or fromLink in time/cost (depends on the way the qsim is implemented...)
    // by default: true (toLinks are included)
    public final static boolean INCLUDE_TO_LINK = true;


    public static class SPEntry
    {
        public final int travelTime;
        public final double travelCost;
        public final Id[] linkIds;


        public SPEntry(int travelTime, double travelCost, Id[] linkIds)
        {
            this.travelTime = travelTime;
            this.travelCost = travelCost;
            this.linkIds = linkIds;
        }
    }


    SPEntry getSPEntry(int departTime);
}
