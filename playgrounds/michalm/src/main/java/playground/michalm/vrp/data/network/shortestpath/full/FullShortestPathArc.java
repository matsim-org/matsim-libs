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

package playground.michalm.vrp.data.network.shortestpath.full;

import pl.poznan.put.util.lang.TimeDiscretizer;
import pl.poznan.put.vrp.dynamic.data.network.InterpolatedArc;
import playground.michalm.vrp.data.network.shortestpath.*;
import playground.michalm.vrp.data.network.shortestpath.ShortestPath.SPEntry;


public class FullShortestPathArc
    extends InterpolatedArc //TODO this interpolation is in conflict with SPEntry
    implements ShortestPathArc
{
    private final FullShortestPath shortestPath;


    public FullShortestPathArc(TimeDiscretizer timeDiscretizer, int[] timesOnDeparture,
            double[] costsOnDeparture, FullShortestPath shortestPath)
    {
        super(timeDiscretizer, timesOnDeparture, costsOnDeparture);
        this.shortestPath = shortestPath;
    }


    @Override
    public ShortestPath getShortestPath()
    {
        return shortestPath;
    }


    public static FullShortestPathArc createArc(TimeDiscretizer timeDiscretizer,
            FullShortestPath shortestPath)
    {
        SPEntry[] entries = shortestPath.entries;
        int numSlots = entries.length;

        int[] timesOnDeparture = new int[numSlots];
        double[] costsOnDeparture = new double[numSlots];

        for (int k = 0; k < numSlots; k++) {
            timesOnDeparture[k] = entries[k].travelTime;
            costsOnDeparture[k] = entries[k].travelCost;
        }

        return new FullShortestPathArc(timeDiscretizer, timesOnDeparture, costsOnDeparture,
                shortestPath);
    }
}
