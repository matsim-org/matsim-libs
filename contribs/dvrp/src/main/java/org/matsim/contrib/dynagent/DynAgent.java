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

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.events.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.*;
import org.matsim.core.mobsim.qsim.interfaces.*;
import org.matsim.core.utils.misc.Time;


public class DynAgent
    implements MobsimDriverAgent
{
    private DynAgentLogic agentLogic;

    private Id id;

    private MobsimVehicle veh;

    private EventsManager events;

    private MobsimAgent.State state;

    // =====

    private DynLeg dynLeg;

    private Id currentLinkId;

    // =====

    private DynActivity dynActivity;


    // =====

    public DynAgent(Id id, Id startLinkId, Netsim netsim, DynAgentLogic agentLogic)
    {
        this.id = id;
        this.currentLinkId = startLinkId;
        this.agentLogic = agentLogic;
        this.events = netsim.getEventsManager();

        // initial activity
        dynActivity = this.agentLogic.computeInitialActivity(this);

        if (dynActivity.getEndTime() != Time.UNDEFINED_TIME) {
            state = MobsimAgent.State.ACTIVITY;
        }
        else {
            state = MobsimAgent.State.ABORT;// ??????
        }
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

            events.processEvent(new ActivityStartEvent(now, id, currentLinkId, null, dynActivity
                    .getActivityType()));
        }
        else {
            dynLeg = (DynLeg)nextDynAction;
            state = MobsimAgent.State.LEG;
        }
    }


    @Override
    public void endActivityAndComputeNextState(double now)
    {
        events.processEvent(new ActivityEndEvent(now, id, currentLinkId, null, dynActivity
                .getActivityType()));

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
    public Id getId()
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


    @Override
    public final Id getPlannedVehicleId()
    {
        if (state != State.LEG) {
            throw new IllegalStateException();// return null;
        }

        // according to PersonDriverAgentImpl:
        // we still assume the vehicleId is the agentId if no vehicleId is given.
        return id;
    }


    @Override
    public void setVehicle(MobsimVehicle veh)
    {
        this.veh = veh;
    }


    @Override
    public MobsimVehicle getVehicle()
    {
        return veh;
    }


    @Override
    public Id getCurrentLinkId()
    {
        return currentLinkId;
    }


    @Override
    public Id getDestinationLinkId()
    {
        return dynLeg.getDestinationLinkId();
    }


    @Override
    public Id chooseNextLinkId()
    {
        return dynLeg.getNextLinkId();
    }


    @Override
    public void notifyMoveOverNode(Id newLinkId)
    {
        dynLeg.movedOverNode(newLinkId);
        currentLinkId = newLinkId;
    }


    @Override
    public double getActivityEndTime()
    {
        return dynActivity.getEndTime();
    }


    public void doSimStep(double now)
    {
        dynActivity.doSimStep(now);
    }


    @Override
    public void notifyArrivalOnLinkByNonNetworkMode(Id linkId)
    {
        dynLeg.arrivedOnLinkByNonNetworkMode(linkId);
        currentLinkId = linkId;
    }


    @Override
    public Double getExpectedTravelTime()
    {
        return dynLeg.getExpectedTravelTime();
    }
}
