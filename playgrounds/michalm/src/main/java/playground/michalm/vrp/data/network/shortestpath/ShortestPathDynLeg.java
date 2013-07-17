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

import org.matsim.api.core.v01.Id;

import pl.poznan.put.vrp.dynamic.data.network.VertexTimePair;
import pl.poznan.put.vrp.dynamic.data.online.VehicleTracker;
import pl.poznan.put.vrp.dynamic.data.schedule.DriveTask;
import playground.michalm.dynamic.DynLeg;
import playground.michalm.vrp.data.network.*;


public class ShortestPathDynLeg
    implements DynLeg
{
    private final ShortestPath shortestPath;

    private final Id originLinkId;
    private final Id destinationLinkId;
    private final int destinationLinkIdx;

    private final int beginTime;

    private int currentLinkIdx = -1;// fromLink idx == -1

    private OnlineVehicleTracker onlineVehicleTracker;


    public ShortestPathDynLeg(DriveTask driveTask)
    {
        beginTime = driveTask.getBeginTime();

        MatsimArc arc = (MatsimArc)driveTask.getArc();
        originLinkId = arc.getFromVertex().getLink().getId();
        destinationLinkId = arc.getToVertex().getLink().getId();

        shortestPath = arc.getShortestPath(beginTime);
        destinationLinkIdx = shortestPath.linkIds.length - 1;
    }


    public void initOnlineVehicleTracker(DriveTask driveTask, MatsimVrpGraph graph)
    {
        if (onlineVehicleTracker == null) {
            onlineVehicleTracker = new OnlineVehicleTracker(graph);
            driveTask.setVehicleTracker(onlineVehicleTracker);
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
        if (currentLinkIdx == -1) {
            return originLinkId;
        }

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
        private final MatsimVrpGraph vrpGraph;

        private int expectedLinkTravelTime = 0;// fromLink travel time == 0

        private int timeAtLatestNode = -1;// has not been reached yet
        private int delayAtLatestNode = 0;// ditto


        public OnlineVehicleTracker(MatsimVrpGraph vrpGraph)
        {
            this.vrpGraph = vrpGraph;
        }


        private void movedOverNode(int time)
        {
            int expectedTimeEnRoute = (currentLinkIdx == 0) ? 0
                    : shortestPath.accLinkTravelTimes[currentLinkIdx - 1];
            int actualTimeEnRoute = timeAtLatestNode - beginTime;

            expectedLinkTravelTime = shortestPath.accLinkTravelTimes[currentLinkIdx]
                    - expectedTimeEnRoute;
            timeAtLatestNode = time;
            delayAtLatestNode = actualTimeEnRoute - expectedTimeEnRoute;
        }


        @Override
        public int calculateCurrentDelay(int currentTime)
        {
            int estimatedDelay = delayAtLatestNode;
            int timeOnLink = currentTime - timeAtLatestNode;

            // delay on the current link
            if (timeOnLink > expectedLinkTravelTime) {
                estimatedDelay += (timeOnLink - expectedLinkTravelTime);
            }

            return estimatedDelay;
        }


        @Override
        public VertexTimePair getLastPosition()
        {
            return new VertexTimePair(vrpGraph.getVertex(getCurrentLinkId()), timeAtLatestNode);
        }


        @Override
        public VertexTimePair predictNextPosition(int currentTime)
        {
            int predictedTimeAtNextNode = timeAtLatestNode
                    + Math.max(currentTime - timeAtLatestNode, expectedLinkTravelTime);
            return new VertexTimePair(vrpGraph.getVertex(getNextLinkId()), predictedTimeAtNextNode);
        }
    }
}
