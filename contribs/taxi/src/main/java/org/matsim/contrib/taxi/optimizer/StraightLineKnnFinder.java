/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.taxi.optimizer;

import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.util.PartialSort;
import org.matsim.contrib.util.distance.DistanceUtils;


public class StraightLineKnnFinder<T, N>
{
    private final int k;
    private final LinkProvider<T> objectToLink;
    private final LinkProvider<N> neighbourToLink;


    public StraightLineKnnFinder(int k, LinkProvider<T> objectToLink,
            LinkProvider<N> neighbourToLink)
    {
        this.k = k;
        this.objectToLink = objectToLink;
        this.neighbourToLink = neighbourToLink;
    }


    public List<N> findNearest(T obj, Iterable<N> neighbours)
    {
        Coord objectCoord = objectToLink.getLink(obj).getCoord();
        PartialSort<N> nearestRequestSort = new PartialSort<N>(k);

        for (N n : neighbours) {
            Coord nCoord = neighbourToLink.getLink(n).getCoord();
            nearestRequestSort.add(n, DistanceUtils.calculateSquaredDistance(objectCoord, nCoord));
        }

        return nearestRequestSort.retriveKSmallestElements();
    }
}
