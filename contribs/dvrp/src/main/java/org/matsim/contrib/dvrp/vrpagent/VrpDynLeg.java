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
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.VrpSimEngine;
import org.matsim.contrib.dvrp.data.schedule.DriveTask;
import org.matsim.contrib.dvrp.router.*;
import org.matsim.contrib.dvrp.tracker.*;
import org.matsim.contrib.dynagent.DynLeg;


/**
 * ASSUMPTION: A vehicle enters and exits links at their ends (link.getToNode())
 */
public class VrpDynLeg
    implements DynLeg
{
    public static VrpDynLeg createLegWithOfflineVehicleTracker(DriveTask driveTask)
    {
        return new VrpDynLeg(driveTask);
    }


    public static VrpDynLeg createLegWithOnlineVehicleTracker(DriveTask driveTask,
            VrpSimEngine vrpSimEngine)
    {
        return new VrpDynLeg(driveTask, vrpSimEngine);
    }


    private final VrpPath path;
    private int currentLinkIdx = 0;
    private final OnlineVehicleTrackerImpl onlineVehicleTracker;


    //DriveTask with OfflineVehicleTrakcer
    private VrpDynLeg(DriveTask driveTask)
    {
        path = driveTask.getPath();
        onlineVehicleTracker = null;
        driveTask.setVehicleTracker(new OfflineVehicleTrackerImpl(driveTask));
    }


    //DriveTask with OnlineVehicleTrakcer; the tracker notifies VrpSimEngine of new positions
    private VrpDynLeg(DriveTask driveTask, VrpSimEngine vrpSimEngine)
    {
        path = driveTask.getPath();
        onlineVehicleTracker = new OnlineVehicleTrackerImpl(driveTask, vrpSimEngine);
        driveTask.setVehicleTracker(onlineVehicleTracker);
    }


    @Override
    public void movedOverNode(Id oldLinkId, Id newLinkId, int time)
    {
        currentLinkIdx++;

        if (onlineVehicleTracker != null) {
            onlineVehicleTracker.movedOverNode(time);
        }
    }


    @Override
    public Id getCurrentLinkId()
    {
        return path.getLinks()[currentLinkIdx].getId();
    }


    @Override
    public Id getNextLinkId()
    {
        Link[] links = path.getLinks();

        if (currentLinkIdx == links.length - 1) {
            return null;
        }

        return links[currentLinkIdx + 1].getId();
    }


    @Override
    public Id getDestinationLinkId()
    {
        return path.getToLink().getId();
    }


    @Override
    public void endAction(double now)
    {}


    private class OnlineVehicleTrackerImpl
        implements OnlineVehicleTracker
    {
        private final DriveTask driveTask;
        private final VrpSimEngine vrpSimEngine;

        private int linkEnterTime;
        private int delayOnLinkEnter;

        private int expectedLinkTravelTime;


        public OnlineVehicleTrackerImpl(DriveTask driveTask, VrpSimEngine vrpSimEngine)
        {
            this.driveTask = driveTask;
            this.vrpSimEngine = vrpSimEngine;

            linkEnterTime = driveTask.getBeginTime();
            delayOnLinkEnter = 0;

            expectedLinkTravelTime = getAccLinkTravelTimes()[0];
        }


        private void movedOverNode(int time)
        {
            linkEnterTime = time;

            int expectedTimeEnRoute = getAccLinkTravelTimes()[currentLinkIdx - 1];
            int actualTimeEnRoute = linkEnterTime - driveTask.getBeginTime();

            delayOnLinkEnter = actualTimeEnRoute - expectedTimeEnRoute;

            expectedLinkTravelTime = getAccLinkTravelTimes()[currentLinkIdx] - expectedTimeEnRoute;

            vrpSimEngine.nextLinkEntered(this);
        }


        @Override
        public DriveTask getDriveTask()
        {
            return driveTask;
        }


        @Override
        public int calculateCurrentDelay(int currentTime)
        {
            int estimatedDelay = delayOnLinkEnter;
            int timeOnLink = currentTime - linkEnterTime;

            // delay on the current link
            if (timeOnLink > expectedLinkTravelTime) {
                estimatedDelay += (timeOnLink - expectedLinkTravelTime);
            }

            return estimatedDelay;
        }


        @Override
        public Link getLink()
        {
            return path.getLinks()[currentLinkIdx];
        }


        @Override
        public int getLinkEnterTime()
        {
            return linkEnterTime;
        }


        @Override
        public int predictLinkExitTime(int currentTime)
        {
            int predictedLinkExitTime = linkEnterTime
                    + Math.max(currentTime - linkEnterTime, expectedLinkTravelTime);
            return predictedLinkExitTime;
        }


        @Override
        public int predictEndTime(int currentTime)
        {
            int[] accLinkTT = getAccLinkTravelTimes();

            int predictedTimeFromNextNode = accLinkTT[accLinkTT.length - 1]
                    - accLinkTT[currentLinkIdx];

            return predictLinkExitTime(currentTime) + predictedTimeFromNextNode;
        }


        @Override
        public int getInitialEndTime()
        {
            int[] accLinkTT = getAccLinkTravelTimes();
            return driveTask.getBeginTime() + accLinkTT[accLinkTT.length - 1];
        }


        private int[] getAccLinkTravelTimes()
        {
            return ((VrpPathImpl)path).getAccLinkTravelTimes();
        }

    }
}
