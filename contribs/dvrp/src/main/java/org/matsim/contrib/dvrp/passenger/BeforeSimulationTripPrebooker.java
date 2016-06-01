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

import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.mobsim.framework.*;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;


/**
 * This does, if I see it correctly, prebook a trip for every passenger already at the beginning of
 * the simulation. For taxis, I would think that it is only useful as a benchmark. It may be
 * realistic for certain types of courier services (where all requests are known before the day
 * starts). kai, jul'14
 */
public class BeforeSimulationTripPrebooker
    implements MobsimInitializedListener
{
    private final PassengerEngine passengerEngine;


    public BeforeSimulationTripPrebooker(PassengerEngine passengerEngine)
    {
        this.passengerEngine = passengerEngine;
    }


    /**
     * TODO Note that in MATSim leg departure times may be meaningless; the only thing that truly
     * matters is the activity end time. kai, jul'14
     */
    @Override
    public void notifyMobsimInitialized(@SuppressWarnings("rawtypes") MobsimInitializedEvent e)
    {
        Collection<MobsimAgent> agents = ((QSim)e.getQueueSimulation()).getAgents();
        String mode = passengerEngine.getMode();

        for (MobsimAgent mobsimAgent : agents) {
            if (mobsimAgent instanceof PlanAgent) {
                Plan plan = ((PlanAgent)mobsimAgent).getCurrentPlan();

                for (PlanElement elem : plan.getPlanElements()) {
                    if (elem instanceof Leg) {
                        Leg leg = (Leg)elem;

                        if (leg.getMode().equals(mode)) {
                            Id<Link> fromLinkId = leg.getRoute().getStartLinkId();
                            Id<Link> toLinkId = leg.getRoute().getEndLinkId();
                            double departureTime = leg.getDepartureTime();
                            passengerEngine.prebookTrip(0, (MobsimPassengerAgent)mobsimAgent,
                                    fromLinkId, toLinkId, departureTime);
                        }
                    }
                }
            }
        }
    }
}
