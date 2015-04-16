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
import org.matsim.contrib.dvrp.optimizer.VrpOptimizerWithOnlineTracking;
import org.matsim.contrib.dvrp.router.*;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.contrib.dvrp.vrpagent.VrpLeg;
import org.matsim.core.mobsim.framework.MobsimTimer;


/**
 * Assumption: vehicle (drive) cannot be redirected from (aborted at) the current link (n).
 * Redirection/abortion is always on next link (n+1). Thus the new and old paths may differ at
 * earliest from link n+2 on...
 * <p>
 * Therefore always use "fresh" Path object, i.e. driveTask.getPath() to get: total travel
 * time&cost, arrival time, or next link and its travel time
 */
class OnlineDriveTaskTrackerImpl
    implements OnlineDriveTaskTracker
{
    private final DriveTask driveTask;
    private final VrpLeg vrpDynLeg;

    private final VrpOptimizerWithOnlineTracking optimizer;
    private final MobsimTimer timer;

    private double linkEnterTime;

    private double plannedTTAtPrevNode;
    private double plannedLinkTT;
    private double plannedEndTime;

    private int currentLinkIdx;
    private Link currentLink;


    OnlineDriveTaskTrackerImpl(DriveTask driveTask, VrpLeg vrpDynLeg,
            VrpOptimizerWithOnlineTracking optimizer, MobsimTimer timer)
    {
        this.driveTask = driveTask;
        this.vrpDynLeg = vrpDynLeg;
        this.optimizer = optimizer;
        this.timer = timer;

        VrpPath path = driveTask.getPath();

        currentLinkIdx = 0;
        currentLink = path.getLink(0);

        linkEnterTime = driveTask.getBeginTime();

        plannedTTAtPrevNode = 0;
        plannedLinkTT = path.getLinkTravelTime(0);
        plannedEndTime = driveTask.getEndTime();
    }


    @Override
    public void movedOverNode()
    {
        VrpPath path = vrpDynLeg.getPath();

        currentLinkIdx++;
        currentLink = path.getLink(currentLinkIdx);

        linkEnterTime = timer.getTimeOfDay();

        plannedTTAtPrevNode += plannedLinkTT;//add previous link TT
        plannedLinkTT = path.getLinkTravelTime(currentLinkIdx);

        optimizer.nextLinkEntered(driveTask);
    }


    @Override
    public LinkTimePair getDiversionPoint(double currentTime)
    {
        if (vrpDynLeg.canChangeNextLink()) {
            return new LinkTimePair(currentLink, predictLinkExitTime(currentTime));
        }

        VrpPath path = vrpDynLeg.getPath();

        if (path.getLinkCount() == currentLinkIdx + 1) {
            return null;//the current link is the last one, but it is too late to divert the vehicle
        }

        Link nextLink = path.getLink(currentLinkIdx + 1);

        double nextLinkTT = path.getLinkTravelTime(currentLinkIdx + 1);
        double predictedDiversionTime = predictLinkExitTime(currentTime) + nextLinkTT;

        return new LinkTimePair(nextLink, predictedDiversionTime);
    }


    public void divertPath(VrpPathWithTravelData newSubPath, double currentTime)
    {
        LinkTimePair diversionPoint = getDiversionPoint(currentTime);

        if (!newSubPath.getFromLink().equals(diversionPoint.link)
                || newSubPath.getDepartureTime() != diversionPoint.time) {//TODO this time check may not work with cached VrpPaths??
            throw new IllegalArgumentException();
        }

        VrpPath originalPath = vrpDynLeg.getPath();

        int diversionLinkIdx = currentLinkIdx + (vrpDynLeg.canChangeNextLink() ? 0 : 1);
        DivertedVrpPath divertedPath = new DivertedVrpPath(originalPath, newSubPath,
                diversionLinkIdx);

        double newEndTime = newSubPath.getArrivalTime();
        vrpDynLeg.pathDiverted(divertedPath);

        //=====================================================================================

        //ASSUMPTION: newSubPath has been calculated at currentTime given that the vehicle will
        //exit the current link at predictLinkExitTime(currentTime)
        //[see getImmediateDiversionPoint() above]. Therefore:

        //1. If vehicle really reaches the end of the link at predictLinkExitTime(currentTime),
        // it means no delays/speedups, i.e. everything happens just on time, as if planned
        plannedTTAtPrevNode = linkEnterTime - newEndTime;
        plannedLinkTT = predictLinkExitTime(currentTime) - linkEnterTime;

        //2. Additionally, memorize the planned end time
        plannedEndTime = newEndTime;
    }


    @Override
    public double predictEndTime(double currentTime)
    {
        double plannedTotalTT = plannedEndTime - driveTask.getBeginTime();
        double predictedRemainingTTFromNextNode = plannedTotalTT
                - (plannedTTAtPrevNode + plannedLinkTT);

        return predictLinkExitTime(currentTime) + predictedRemainingTTFromNextNode;
    }


    private double predictLinkExitTime(double currentTime)
    {
        return Math.max(currentTime, linkEnterTime + plannedLinkTT);
    }
}