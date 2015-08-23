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

package org.matsim.contrib.dvrp.vrpagent;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.router.*;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;


/**
 * ASSUMPTION: A vehicle enters and exits links at their ends (link.getToNode())
 */
public class VrpLeg
    implements DivertibleLeg
{
    private OnlineDriveTaskTracker onlineTracker;

    private VrpPath path;
    private int currentLinkIdx = 0;
    private boolean askedAboutNextLink = false;

    private final String mode = TransportMode.car;//TODO


    public VrpLeg(VrpPath path)
    {
        this.path = path;
    }


    public void initOnlineTracking(OnlineDriveTaskTracker onlineTracker)
    {
        if (this.onlineTracker != null) {
            throw new IllegalStateException("Tracking already initialized");
        }

        if (currentLinkIdx != 0) {
            throw new IllegalStateException("Too late for initializing online tracking");
        }

        this.onlineTracker = onlineTracker;
    }


    @Override
    public void movedOverNode(Id<Link> newLinkId)
    {
        currentLinkIdx++;
        askedAboutNextLink = false;

        if (path.getLink(currentLinkIdx).getId() != newLinkId) {
            throw new IllegalStateException();
        }

        if (onlineTracker != null) {
            onlineTracker.movedOverNode();
        }
    }


    @Override
    public boolean canChangeNextLink()
    {
        return !askedAboutNextLink;
    }


    @Override
    public void pathDiverted(DivertedVrpPath divertedPath)
    {
        int immediateDiversionLinkIdx = currentLinkIdx + (canChangeNextLink() ? 0 : 1);
        if (divertedPath.getDiversionLinkIdx() < immediateDiversionLinkIdx) {
            throw new IllegalStateException();
        }

        //divertedPath must be derived from the original one 
        if (divertedPath.getOriginalPath() != path) {
            throw new IllegalArgumentException();
        }

        path = divertedPath;
    }


    @Override
    public VrpPath getPath()
    {
        return path;
    }


    @Override
    public Id<Link> getNextLinkId()
    {
        askedAboutNextLink = true;

        if (currentLinkIdx == path.getLinkCount() - 1) {
            return null;
        }

        return path.getLink(currentLinkIdx + 1).getId();
    }


    @Override
    public Id<Link> getDestinationLinkId()
    {
        return path.getToLink().getId();
    }


    @Override
    public void finalizeAction(double now)
    {}


    @Override
    public String getMode()
    {
        return mode;
    }


    @Override
    public void arrivedOnLinkByNonNetworkMode(Id<Link> linkId)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public Double getExpectedTravelTime()
    {
        return null;//teleportation is not handled
    }


    @Override
    public Double getExpectedTravelDistance()
    {
        return null;//teleportation is not handled
    }
}
