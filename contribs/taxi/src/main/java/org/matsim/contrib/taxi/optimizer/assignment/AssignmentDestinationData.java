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

import org.matsim.api.core.v01.network.Link;


public abstract class AssignmentDestinationData<D>
{
    public static class DestEntry<D>
    {
        public final int idx;
        public final D destination;
        public final Link link;
        public final double time;


        public DestEntry(int idx, D destination, Link link, double time)
        {
            this.idx = idx;
            this.destination = destination;
            this.link = link;
            this.time = time;
        }
    }


    protected final List<DestEntry<D>> entries = new ArrayList<>();


    public int getSize()
    {
        return entries.size();
    }


    public DestEntry<D> getEntry(int idx)
    {
        return entries.get(idx);
    }


    public List<DestEntry<D>> getEntries()
    {
        return entries;
    }
}