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
import playground.michalm.vrp.data.network.shortestpath.ShortestPath;


public class FullShortestPath
    implements ShortestPath
{
    private final TimeDiscretizer timeDiscretizer;

    final SPEntry entries[];


    public FullShortestPath(TimeDiscretizer timeDiscretizer)
    {
        this.timeDiscretizer = timeDiscretizer;
        entries = new SPEntry[timeDiscretizer.getIntervalCount()];
    }


    @Override
    public SPEntry getSPEntry(int departTime)
    {
        return entries[timeDiscretizer.getIdx(departTime)];
    }
}
