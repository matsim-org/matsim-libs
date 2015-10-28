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

package org.matsim.contrib.dvrp.path;

import java.util.Iterator;

import org.matsim.api.core.v01.network.Link;

import com.google.common.collect.Iterators;


public class DivertedVrpPath
    implements VrpPath
{
    private final VrpPath originalPath;
    private final VrpPath newSubPath;
    private final int diversionLinkIdx;// originalPath.getLink(diversionLinkIdx) == newSubPath.getLink(0)


    public DivertedVrpPath(VrpPath originalPath, VrpPath newSubPath, int diversionLinkIdx)
    {
        if (originalPath.getLink(diversionLinkIdx) != newSubPath.getLink(0)) {
            throw new IllegalArgumentException();
        }

        this.originalPath = originalPath;
        this.newSubPath = newSubPath;
        this.diversionLinkIdx = diversionLinkIdx;
    }


    @Override
    public int getLinkCount()
    {
        return diversionLinkIdx + newSubPath.getLinkCount();
    }


    @Override
    public Link getLink(int idx)
    {
        if (idx <= diversionLinkIdx) {//equivalent to: idx < diversionLinkIdx
            return originalPath.getLink(idx);
        }
        else {
            return newSubPath.getLink(idx - diversionLinkIdx);
        }
    }


    @Override
    public double getLinkTravelTime(int idx)
    {
        //TT for diversionLinkIdx must be taken from originalPath since TT for the first link
        //in newSubPath is 1 second (a vehicle enters the link at its end)  

        if (idx <= diversionLinkIdx) {//incorrect: idx < diversionLinkIdx
            return originalPath.getLinkTravelTime(idx);
        }
        else {
            return newSubPath.getLinkTravelTime(idx - diversionLinkIdx);
        }
    }


    @Override
    public Link getFromLink()
    {
        return originalPath.getFromLink();
    }


    @Override
    public Link getToLink()
    {
        return newSubPath.getToLink();
    }


    @Override
    public Iterator<Link> iterator()
    {
        return Iterators.concat(Iterators.limit(originalPath.iterator(), diversionLinkIdx),
                newSubPath.iterator());
    }


    public VrpPath getOriginalPath()
    {
        return originalPath;
    }


    public VrpPath getNewSubPath()
    {
        return newSubPath;
    }


    public int getDiversionLinkIdx()
    {
        return diversionLinkIdx;
    }
}
