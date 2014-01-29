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

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.router.*;
import org.matsim.contrib.dvrp.tracker.OnlineVehicleTracker;


/**
 * ASSUMPTION: A vehicle enters and exits links at their ends (link.getToNode())
 */
public class VrpDynLeg
    implements DivertibleDynLeg
{
    private final OnlineVehicleTracker onlineVehicleTracker;

    private VrpPath path;
    private int currentLinkIdx = 0;
    private boolean askedAboutNextLink = false;


    //DriveTask with OfflineVehicleTrakcer
    /*package*/VrpDynLeg(VrpPath path)
    {
        this.path = path;
        this.onlineVehicleTracker = null;
    }


    //DriveTask with OnlineVehicleTrakcer; the tracker notifies VrpSimEngine of new positions
    /*package*/VrpDynLeg(VrpPath path, OnlineVehicleTracker onlineVehicleTracker)
    {
        this.path = path;
        this.onlineVehicleTracker = onlineVehicleTracker;
    }


    @Override
    public void movedOverNode(Id newLinkId)
    {
        currentLinkIdx++;
        askedAboutNextLink = false;

        if (path.getLink(currentLinkIdx).getId() != newLinkId) {
            throw new IllegalStateException();
        }

        if (onlineVehicleTracker != null) {
            onlineVehicleTracker.movedOverNode();
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

        path = divertedPath;
    }


    @Override
    public VrpPath getPath()
    {
        return path;
    }


    @Override
    public Id getCurrentLinkId()
    {
        return path.getLink(currentLinkIdx).getId();
    }


    @Override
    public Id getNextLinkId()
    {
        askedAboutNextLink = true;

        if (currentLinkIdx == path.getLinkCount() - 1) {
            return null;
        }

        return path.getLink(currentLinkIdx + 1).getId();
    }


    @Override
    public Id getDestinationLinkId()
    {
        return path.getToLink().getId();
    }


    @Override
    public void finalizeAction(double now)
    {}
}
