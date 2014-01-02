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
import org.matsim.contrib.dvrp.data.network.MatsimArc;
import org.matsim.contrib.dvrp.data.network.shortestpath.ShortestPath;
import org.matsim.contrib.dynagent.DynLeg;

import pl.poznan.put.vrp.dynamic.data.online.VehicleTracker;
import pl.poznan.put.vrp.dynamic.data.schedule.DriveTask;


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


    private final ShortestPath shortestPath;

    private final Id destinationLinkId;
    private final int destinationLinkIdx;

    private int currentLinkIdx = 0;

    private final OnlineVehicleTracker onlineVehicleTracker;


    //DriveTask with OfflineVehicleTrakcer
    private VrpDynLeg(DriveTask driveTask)
    {
        MatsimArc arc = (MatsimArc)driveTask.getArc();
        destinationLinkId = arc.getToLink().getId();
        shortestPath = arc.getShortestPath(driveTask.getBeginTime());
        destinationLinkIdx = shortestPath.links.length - 1;

        onlineVehicleTracker = null;//offlineVehicleTracker is used in DriveTask by default 
    }


    //DriveTask with OnlineVehicleTrakcer; the tracker notifies VrpSimEngine of new positions
    private VrpDynLeg(DriveTask driveTask, VrpSimEngine vrpSimEngine)
    {
        MatsimArc arc = (MatsimArc)driveTask.getArc();
        destinationLinkId = arc.getToLink().getId();
        shortestPath = arc.getShortestPath(driveTask.getBeginTime());
        destinationLinkIdx = shortestPath.links.length - 1;

        onlineVehicleTracker = new OnlineVehicleTracker(driveTask, vrpSimEngine);
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
        return shortestPath.links[currentLinkIdx].getId();
    }


    @Override
    public Id getNextLinkId()
    {
        if (currentLinkIdx == destinationLinkIdx) {
            return null;
        }

        return shortestPath.links[currentLinkIdx + 1].getId();
    }


    @Override
    public Id getDestinationLinkId()
    {
        return destinationLinkId;
    }


    @Override
    public void endAction(double now)
    {}


    private class OnlineVehicleTracker
        implements VehicleTracker
    {
        private final DriveTask driveTask;
        private final VrpSimEngine vrpSimEngine;

        private int timeAtLastNode;
        private int delayAtLastNode;

        private int expectedLinkTravelTime;


        public OnlineVehicleTracker(DriveTask driveTask, VrpSimEngine vrpSimEngine)
        {
            this.driveTask = driveTask;
            this.vrpSimEngine = vrpSimEngine;

            timeAtLastNode = driveTask.getBeginTime();
            delayAtLastNode = 0;

            expectedLinkTravelTime = shortestPath.accLinkTravelTimes[0];
        }


        private void movedOverNode(int time)
        {
            timeAtLastNode = time;

            int expectedTimeEnRoute = shortestPath.accLinkTravelTimes[currentLinkIdx - 1];
            int actualTimeEnRoute = timeAtLastNode - driveTask.getBeginTime();

            delayAtLastNode = actualTimeEnRoute - expectedTimeEnRoute;

            expectedLinkTravelTime = shortestPath.accLinkTravelTimes[currentLinkIdx]
                    - expectedTimeEnRoute;

            vrpSimEngine.nextPositionReached(this);
        }


        @Override
        public DriveTask getDriveTask()
        {
            return driveTask;
        }


        @Override
        public int calculateCurrentDelay(int currentTime)
        {
            int estimatedDelay = delayAtLastNode;
            int timeOnLink = currentTime - timeAtLastNode;

            // delay on the current link
            if (timeOnLink > expectedLinkTravelTime) {
                estimatedDelay += (timeOnLink - expectedLinkTravelTime);
            }

            return estimatedDelay;
        }


        @Override
        public Link getLink()
        {
            if (currentLinkIdx == 0) {
                return null;//the vehicle is at the very beginning (before the first node)
            }

            return shortestPath.links[currentLinkIdx - 1];
        }


        @Override
        public int getLinkEnterTime()
        {
            return timeAtLastNode;
        }


        @Override
        public int predictLinkExitTime(int currentTime)
        {
            int predictedTimeAtNextNode = timeAtLastNode
                    + Math.max(currentTime - timeAtLastNode, expectedLinkTravelTime);
            return predictedTimeAtNextNode;
        }


        @Override
        public int predictEndTime(int currentTime)
        {
            int predictedTimeFromNextNode = shortestPath.accLinkTravelTimes[destinationLinkIdx]
                    - shortestPath.accLinkTravelTimes[currentLinkIdx];

            return predictLinkExitTime(currentTime) + predictedTimeFromNextNode;
        }


        @Override
        public int getInitialEndTime()
        {
            return driveTask.getBeginTime() + shortestPath.accLinkTravelTimes[destinationLinkIdx];
        }
    }
}
