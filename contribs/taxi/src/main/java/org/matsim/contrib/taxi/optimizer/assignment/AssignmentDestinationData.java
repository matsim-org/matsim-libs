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

package org.matsim.contrib.taxi.optimizer.assignment;

import java.util.*;

import org.matsim.api.core.v01.*;


//TODO consider <D> instead of <D extends Identifiable<I>, I> to make it more general 
public class AssignmentDestinationData<D extends Identifiable<I>, I>
{
    private final List<D> destinations = new ArrayList<>();
    private final Map<Id<I>, Integer> destIdx = new HashMap<>();
    private int size;


    public void init(SortedSet<D> candidateDestinations)
    {
        int idx = 0;
        for (D d : candidateDestinations) {
            if (doIncludeDestination(d)) {
                destinations.add(d);
                destIdx.put(d.getId(), idx++);
            }
        }

        size = destinations.size();
    }


    protected boolean doIncludeDestination(D destination)
    {
        return true;
    }


    public int getSize()
    {
        return size;
    }


    public D getDestination(int idx)
    {
        return destinations.get(idx);
    }


    public List<D> getDestinations()
    {
        return destinations;
    }


    public int getIdx(D destination)
    {
        return destIdx.get(destination.getId());
    }
}