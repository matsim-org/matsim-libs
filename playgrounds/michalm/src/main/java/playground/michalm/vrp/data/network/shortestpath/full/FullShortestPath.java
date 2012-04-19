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

import playground.michalm.vrp.data.network.shortestpath.ShortestPath;


public class FullShortestPath
    implements ShortestPath
{
    private int timeInterval;
    private int intervalCoutn;
    private boolean cyclic;

    SPEntry entries[];


    public FullShortestPath(int numIntervals, int timeInterval, boolean cyclic)
    {
        this.timeInterval = timeInterval;
        this.intervalCoutn = numIntervals;
        this.cyclic = cyclic;

        entries = new SPEntry[numIntervals];
    }


    private int getIdx(int departTime)
    {
        int idx = (departTime / timeInterval);
        return cyclic ? (idx % intervalCoutn) : idx;
    }


    @Override
    public SPEntry getSPEntry(int departTime)
    {
        return entries[getIdx(departTime)];
    }
}
