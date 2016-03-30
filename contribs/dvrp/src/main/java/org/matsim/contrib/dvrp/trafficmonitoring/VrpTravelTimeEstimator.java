/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.trafficmonitoring;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.*;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.vehicles.Vehicle;

import com.google.common.collect.Maps;
import com.google.inject.Inject;


//TODO TTC is more flexible (simple averaging vs linear interpolation)
public class VrpTravelTimeEstimator
    implements TravelTime, MobsimBeforeCleanupListener
{
    private final TravelTimeCalculator ttCalc;//observed travel times
    private final Network network;
    private final int interval;
    private final int intervalCount;
    private final Map<Id<Link>, double[]> linkTTs;


    @Inject
    public VrpTravelTimeEstimator(TravelTimeCalculator calculator, Network network)
    {
        this.ttCalc = calculator;
        this.network = network;

        this.interval = ttCalc.getTimeSlice();
        this.intervalCount = ttCalc.getNumSlots();

        linkTTs = Maps.newHashMapWithExpectedSize(network.getLinks().size());
        for (Link link : network.getLinks().values()) {
            double[] tt = new double[intervalCount];
            updateTTs(link, tt);
            linkTTs.put(link.getId(), tt);
        }
    }


    @Override
    public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle)
    {
        //the last time bin in TTC is used for a freely large time  
        int idx = Math.min((int) (time / interval), intervalCount - 1);
        return linkTTs.get(link.getId())[idx];
    }


    @Override
    public void notifyMobsimBeforeCleanup(@SuppressWarnings("rawtypes") MobsimBeforeCleanupEvent e)
    {
        for (Link link : network.getLinks().values()) {
            updateTTs(link, linkTTs.get(link.getId()));
        }
    }


    private void updateTTs(Link link, double[] tt)
    {
        for (int i = 0; i < intervalCount; i++) {
            tt[i] = ttCalc.getLinkTravelTime(link.getId(), i * interval);
        }
    }
}
