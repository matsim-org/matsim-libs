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

package playground.michalm.vrp.data.network.shortestpath;

import java.util.*;

import org.matsim.api.core.v01.Id;

import pl.poznan.put.vrp.dynamic.data.network.Vertex;
import pl.poznan.put.vrp.dynamic.data.online.*;
import pl.poznan.put.vrp.dynamic.data.schedule.DriveTask;
import playground.michalm.dynamic.DynLeg;
import playground.michalm.vrp.data.network.*;


/**
 * ASSUMPTION: A vehicle enters and exits links at their ends (link.getToNode())
 */
public class ShortestPathDynLeg
    implements DynLeg
{
    private final ShortestPath shortestPath;

    private final Id destinationLinkId;
    private final int destinationLinkIdx;

    private final int beginTime;

    private int currentLinkIdx = 0;

    private OnlineVehicleTracker onlineVehicleTracker;


    public ShortestPathDynLeg(DriveTask driveTask)
    {
        beginTime = driveTask.getBeginTime();

        MatsimArc arc = (MatsimArc)driveTask.getArc();
        destinationLinkId = arc.getToVertex().getLink().getId();
        shortestPath = arc.getShortestPath(beginTime);
        destinationLinkIdx = shortestPath.linkIds.length - 1;
    }


    public void initOnlineVehicleTracker(DriveTask driveTask, MatsimVrpGraph graph,
            VehicleTrackerListener listener)
    {
        if (onlineVehicleTracker == null) {
            onlineVehicleTracker = new OnlineVehicleTracker(driveTask, graph);
            driveTask.setVehicleTracker(onlineVehicleTracker);
            onlineVehicleTracker.addListener(listener);
        }
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
        return shortestPath.linkIds[currentLinkIdx];
    }


    @Override
    public Id getNextLinkId()
    {
        if (currentLinkIdx == destinationLinkIdx) {
            return null;
        }

        return shortestPath.linkIds[currentLinkIdx + 1];
    }


    @Override
    public Id getDestinationLinkId()
    {
        return destinationLinkId;
    }


    private class OnlineVehicleTracker
        implements VehicleTracker
    {
        private final DriveTask driveTask;
        private final MatsimVrpGraph vrpGraph;

        private int timeAtLastNode = beginTime;
        private int delayAtLastNode = 0;

        private int expectedLinkTravelTime = shortestPath.accLinkTravelTimes[0];

        private List<VehicleTrackerListener> listeners = new ArrayList<VehicleTrackerListener>();


        public OnlineVehicleTracker(DriveTask driveTask, MatsimVrpGraph vrpGraph)
        {
            this.driveTask = driveTask;
            this.vrpGraph = vrpGraph;
        }


        private void movedOverNode(int time)
        {
            timeAtLastNode = time;

            int expectedTimeEnRoute = shortestPath.accLinkTravelTimes[currentLinkIdx - 1];
            int actualTimeEnRoute = timeAtLastNode - beginTime;

            delayAtLastNode = actualTimeEnRoute - expectedTimeEnRoute;
            expectedLinkTravelTime = shortestPath.accLinkTravelTimes[currentLinkIdx]
                    - expectedTimeEnRoute;

            for (VehicleTrackerListener l : listeners) {
                l.nextPositionReached(this);
            }
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
        public Vertex getLastPosition()
        {
            if (currentLinkIdx == 0) {
                return null;//the vehicle is at the very beginning (before the first node)
            }

            return vrpGraph.getVertex(shortestPath.linkIds[currentLinkIdx - 1]);
        }


        @Override
        public int getLastPositionTime()
        {
            return timeAtLastNode;
        }


        @Override
        public Vertex predictNextPosition(int currentTime)
        {
            return vrpGraph.getVertex(shortestPath.linkIds[currentLinkIdx]);
        }


        @Override
        public int predictNextPositionTime(int currentTime)
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

            return predictNextPositionTime(currentTime) + predictedTimeFromNextNode;
        }


        @Override
        public void addListener(VehicleTrackerListener listener)
        {
            listeners.add(listener);
        }


        @Override
        public void removeListener(VehicleTrackerListener listener)
        {
            listeners.remove(listener);
        }
    }
}
