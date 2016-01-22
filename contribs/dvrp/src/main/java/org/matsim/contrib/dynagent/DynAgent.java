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

package org.matsim.contrib.dynagent;

import java.util.List;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.interfaces.*;
import org.matsim.core.mobsim.qsim.pt.*;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.Vehicle;


public class DynAgent
    implements MobsimDriverPassengerAgent
{
    private DynAgentLogic agentLogic;

    private Id<Person> id;

    private MobsimVehicle veh;

    private EventsManager events;

    private MobsimAgent.State state;

    // =====

    private DynLeg dynLeg;

    private Id<Link> currentLinkId;

    // =====

    private DynActivity dynActivity;


    // =====

    public DynAgent(Id<Person> id, Id<Link> startLinkId, Netsim netsim, DynAgentLogic agentLogic)
    {
        this.id = id;
        this.currentLinkId = startLinkId;
        this.agentLogic = agentLogic;
        this.events = netsim.getEventsManager();

        // initial activity
        dynActivity = this.agentLogic.computeInitialActivity(this);
        state = dynActivity.getEndTime() != Time.UNDEFINED_TIME ? //
                MobsimAgent.State.ACTIVITY : MobsimAgent.State.ABORT;
    }


    private void computeNextAction(DynAction oldDynAction, double now)
    {
        oldDynAction.finalizeAction(now);

        state = null;// !!! this is important
        dynActivity = null;
        dynLeg = null;

        DynAction nextDynAction = agentLogic.computeNextAction(oldDynAction, now);

        if (nextDynAction instanceof DynActivity) {
            dynActivity = (DynActivity)nextDynAction;
            state = MobsimAgent.State.ACTIVITY;

            events.processEvent(new ActivityStartEvent(now, id, currentLinkId, null,
                    dynActivity.getActivityType()));
        }
        else {
            dynLeg = (DynLeg)nextDynAction;
            state = MobsimAgent.State.LEG;
        }
    }


    @Override
    public void endActivityAndComputeNextState(double now)
    {
        events.processEvent(
                new ActivityEndEvent(now, id, currentLinkId, null, dynActivity.getActivityType()));

        computeNextAction(dynActivity, now);
    }


    @Override
    public void endLegAndComputeNextState(double now)
    {
        events.processEvent(new PersonArrivalEvent(now, id, currentLinkId, TransportMode.car));

        computeNextAction(dynLeg, now);
    }


    @Override
    public void setStateToAbort(double now)
    {
        this.state = MobsimAgent.State.ABORT;
    }


    public DynAgentLogic getAgentLogic()
    {
        return agentLogic;
    }


    @Override
    public Id<Person> getId()
    {
        return id;
    }


    @Override
    public MobsimAgent.State getState()
    {
        return this.state;
    }


    @Override
    public String getMode()
    {
        return (state == State.LEG) ? dynLeg.getMode() : null;
    }


    //VehicleUsingAgent
    @Override
    public final Id<Vehicle> getPlannedVehicleId()
    {
        if (state != State.LEG) {
            throw new IllegalStateException();// return null;
        }

        // according to PersonDriverAgentImpl:
        // we still assume the vehicleId is the agentId if no vehicleId is given.
        return Id.create(id, Vehicle.class);
    }


    //VehicleUsingAgent
    @Override
    public void setVehicle(MobsimVehicle veh)
    {
        this.veh = veh;
    }


    //VehicleUsingAgent
    @Override
    public MobsimVehicle getVehicle()
    {
        return veh;
    }


    //NetworkAgent
    @Override
    public Id<Link> getCurrentLinkId()
    {
        return currentLinkId;
    }


    //NetworkAgent (used only for teleportation)
    @Override
    public Id<Link> getDestinationLinkId()
    {
        return dynLeg.getDestinationLinkId();
    }


    //DriverAgent
    @Override
    public Id<Link> chooseNextLinkId()
    {
        return ((DriverDynLeg)dynLeg).getNextLinkId();
    }


    //DriverAgent
    @Override
    public void notifyMoveOverNode(Id<Link> newLinkId)
    {
        ((DriverDynLeg)dynLeg).movedOverNode(newLinkId);
        currentLinkId = newLinkId;
    }


    //MobsimAgent
    @Override
    public double getActivityEndTime()
    {
        return dynActivity.getEndTime();
    }


    //DynAgent
    public void doSimStep(double now)
    {
        dynActivity.doSimStep(now);
    }


    //MobsimAgent
    @Override
    public void notifyArrivalOnLinkByNonNetworkMode(Id<Link> linkId)
    {
        dynLeg.arrivedOnLinkByNonNetworkMode(linkId);
        currentLinkId = linkId;
    }


    //MobsimAgent
    @Override
    public Double getExpectedTravelTime()
    {
        return dynLeg.getExpectedTravelTime();
    }


    //MobsimAgent
    @Override
    public Double getExpectedTravelDistance()
    {
        return dynLeg.getExpectedTravelDistance();
    }


    //DriverAgent
    @Override
    public boolean isWantingToArriveOnCurrentLink()
    {
        return chooseNextLinkId() == null;
    }


    //PTPassengerAgent
    @Override
    public boolean getEnterTransitRoute(TransitLine line, TransitRoute transitRoute,
            List<TransitRouteStop> stopsToCome, TransitVehicle transitVehicle)
    {
        return ((PTPassengerDynLeg)dynLeg).getEnterTransitRoute(line, transitRoute, stopsToCome,
                transitVehicle);
    }


    //PTPassengerAgent
    @Override
    public boolean getExitAtStop(TransitStopFacility stop)
    {
        return ((PTPassengerDynLeg)dynLeg).getExitAtStop(stop);
    }


    //PTPassengerAgent
    @Override
    public Id<TransitStopFacility> getDesiredAccessStopId()
    {
        return ((PTPassengerDynLeg)dynLeg).getDesiredAccessStopId();
    }


    //PTPassengerAgent
    @Override
    public Id<TransitStopFacility> getDesiredDestinationStopId()
    {
        return ((PTPassengerDynLeg)dynLeg).getDesiredDestinationStopId();
    }


    //PTPassengerAgent
    @Override
    public double getWeight()
    {
        return 1;
    }
}
