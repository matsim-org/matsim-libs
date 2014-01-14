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

package org.matsim.contrib.dvrp.tracker;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.VrpSimEngine;
import org.matsim.contrib.dvrp.data.schedule.DriveTask;
import org.matsim.contrib.dvrp.router.*;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.contrib.dvrp.vrpagent.VrpDynLeg;


/**
 * Assumption: vehicle (drive) cannot be redirected from (aborted at) the current link (n).
 * Redirection/abortion is always on next link (n+1). Thus the new and old paths may differ at
 * earliest from link n+2 on...
 * <p>
 * Therefore always use "fresh" Path object, i.e. driveTask.getPath() to get: total travel
 * time&cost, arrival time, or next link and its travel time
 */
public class OnlineVehicleTrackerImpl
    implements OnlineVehicleTracker
{
    private final DriveTask driveTask;
    private final VrpSimEngine vrpSimEngine;
    private VrpDynLeg vrpDynLeg;

    private int linkEnterTime;
    private int linkEnterDelay;

    private int plannedTTAtPrevNode;
    private int plannedLinkTT;
    private int plannedEndTime;

    private int currentLinkIdx;
    private Link currentLink;


    public OnlineVehicleTrackerImpl(DriveTask driveTask, VrpSimEngine vrpSimEngine)
    {
        this.driveTask = driveTask;
        this.vrpSimEngine = vrpSimEngine;

        VrpPath path = driveTask.getPath();

        currentLinkIdx = 0;
        currentLink = path.getLink(0);

        linkEnterTime = driveTask.getBeginTime();

        plannedTTAtPrevNode = 0;
        plannedLinkTT = path.getLinkTravelTime(0);
        plannedEndTime = driveTask.getEndTime();

        linkEnterDelay = 0;
    }


    @Override
    public void movedOverNode(int time)
    {
        VrpPath path = driveTask.getPath();

        currentLinkIdx++;
        currentLink = path.getLink(currentLinkIdx);

        linkEnterTime = time;

        plannedTTAtPrevNode += plannedLinkTT;//add previous link TT
        plannedLinkTT = path.getLinkTravelTime(currentLinkIdx);

        int actualTTAtPrevNode = linkEnterTime - driveTask.getBeginTime();
        linkEnterDelay = actualTTAtPrevNode - plannedTTAtPrevNode;

        vrpSimEngine.nextLinkEntered(this);
    }


    @Override
    public LinkTimePair getDiversionPoint(int currentTime)
    {
        if (vrpDynLeg.canChangeNextLink()) {
            return new LinkTimePair(currentLink, predictLinkExitTime(currentTime));
        }

        VrpPath path = driveTask.getPath();

        if (path.getLinkCount() == currentLinkIdx + 1) {
            return null;//the current link is the last one, but it is too late to divert the vehicle
        }

        Link nextLink = path.getLink(currentLinkIdx + 1);

        int nextLinkTT = path.getLinkTravelTime(currentLinkIdx + 1);
        int predictedDiversionTime = predictLinkExitTime(currentTime) + nextLinkTT;

        return new LinkTimePair(nextLink, predictedDiversionTime);
    }


    public void divertPath(VrpPathWithTravelData newSubPath, int currentTime)
    {
        LinkTimePair diversionPoint = getDiversionPoint(currentTime);

        if (!newSubPath.getFromLink().equals(diversionPoint.link)
                || newSubPath.getDepartureTime() != diversionPoint.time) {
            throw new IllegalArgumentException();
        }

        VrpPath originalPath = driveTask.getPath();

        int diversionLinkIdx = currentLinkIdx + (vrpDynLeg.canChangeNextLink() ? 0 : 1);
        DivertedVrpPath divertedPath = new DivertedVrpPath(originalPath, newSubPath,
                diversionLinkIdx);

        vrpDynLeg.pathDiverted(divertedPath);

        //ASSUMPTION: newSubPath has been calculated at currentTime given that the vehicle will
        //exit the current link at predictLinkExitTime(currentTime)
        //[see getImmediateDiversionPoint() above]. Therefore:

        //1. If vehicle really reaches the end of the link at predictLinkExitTime(currentTime),
        // it means no delays/speedups, i.e. everything happens just on time, as if planned
        plannedTTAtPrevNode = linkEnterTime - driveTask.getBeginTime();
        linkEnterDelay = 0;
        plannedLinkTT = predictLinkExitTime(currentTime) - linkEnterTime;

        //2. Additionally, memorize the planned end time
        plannedEndTime = driveTask.getEndTime();

    }


    @Override
    public int calculateCurrentDelay(int currentTime)
    {
        int actualTimeOnLink = currentTime - linkEnterTime;

        // delay(positive/zero/negative) at the last node + delay(positive/zero) on the current link 
        return linkEnterDelay + Math.max(actualTimeOnLink - plannedLinkTT, 0);
    }


    @Override
    public Link getLink()
    {
        return currentLink;
    }


    @Override
    public int getLinkEnterTime()
    {
        return linkEnterTime;
    }


    @Override
    public int predictLinkExitTime(int currentTime)
    {
        return Math.max(currentTime, linkEnterTime + plannedLinkTT);
    }


    @Override
    public int predictEndTime(int currentTime)
    {
        int plannedTotalTT = plannedEndTime - driveTask.getBeginTime();
        int predictedRemainingTTFromNextNode = plannedTotalTT
                - (plannedTTAtPrevNode + plannedLinkTT);

        return predictLinkExitTime(currentTime) + predictedRemainingTTFromNextNode;
    }


    @Override
    public int getPlannedEndTime()
    {
        return plannedEndTime;
    }


    @Override
    public DriveTask getDriveTask()
    {
        return driveTask;
    }


    @Override
    public void setVrpDynLeg(VrpDynLeg vrpDynLeg)
    {
        this.vrpDynLeg = vrpDynLeg;
    }
}