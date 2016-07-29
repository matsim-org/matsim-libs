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

package org.matsim.contrib.dvrp.passenger;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;
import org.matsim.core.mobsim.framework.events.*;
import org.matsim.core.mobsim.framework.listeners.*;
import org.matsim.core.mobsim.qsim.QSim;


public class TripPrebookingManager
    implements MobsimInitializedListener, MobsimBeforeSimStepListener
{
    private static class PrebookingEntry
    {
        private final double submissionTime;
        private final MobsimPassengerAgent passenger;
        private final Id<Link> fromLinkId;
        private final Id<Link> toLinkId;
        private final double departureTime;


        private PrebookingEntry(double submissionTime, MobsimPassengerAgent passenger,
                Id<Link> fromLinkId, Id<Link> toLinkId, double departureTime)
        {
            this.submissionTime = submissionTime;
            this.passenger = passenger;
            this.fromLinkId = fromLinkId;
            this.toLinkId = toLinkId;
            this.departureTime = departureTime;
        }
    }


    public static final Comparator<PrebookingEntry> BOOKING_COMPARATOR = new Comparator<PrebookingEntry>() {
        public int compare(PrebookingEntry p1, PrebookingEntry p2)
        {
            return Double.compare(p1.submissionTime, p2.submissionTime);
        }
    };

    private final PassengerEngine passengerEngine;
    private final Queue<PrebookingEntry> prebookingQueue = new PriorityQueue<>(111,
            BOOKING_COMPARATOR);


    public TripPrebookingManager(PassengerEngine passengerEngine)
    {
        this.passengerEngine = passengerEngine;
    }


    public void scheduleTripPrebooking(double submissionTime, MobsimPassengerAgent passenger,
            Id<Link> fromLinkId, Id<Link> toLinkId, double departureTime)
    // yy I don't think that this is called from anywhere right now.  This is consistent with the statement that
    // call-head is currently not investigated; the only thing that is investigated is call-before-day-starts
    // (as benchmark).  kai, jul'14
    {
        PrebookingEntry prebooking = new PrebookingEntry(submissionTime, passenger, fromLinkId,
                toLinkId, departureTime);
        prebookingQueue.add(prebooking);
    }


    @Override
    public void notifyMobsimInitialized(@SuppressWarnings("rawtypes") MobsimInitializedEvent e)
    {
        double startTime = ((QSim)e.getQueueSimulation()).getSimTimer().getSimStartTime();
        prebookTrips(startTime - 1);
    }


    @Override
    public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e)
    {
        prebookTrips(e.getSimulationTime());
    }


    /**
     * Goes through the prebookingQueue and submits all prebooking requests which are due "now"
     * 
     * @param now
     */
    private void prebookTrips(double now)
    {
        while (prebookingQueue.peek().submissionTime <= now) {
            PrebookingEntry pe = prebookingQueue.poll();
            passengerEngine.prebookTrip(now, pe.passenger, pe.fromLinkId, pe.toLinkId,
                    pe.departureTime);
        }
    }
}
