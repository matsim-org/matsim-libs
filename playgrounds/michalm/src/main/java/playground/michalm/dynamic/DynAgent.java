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

package playground.michalm.dynamic;

import org.matsim.api.core.v01.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.*;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.utils.misc.Time;


public class DynAgent
    implements MobsimDriverAgent
{
    private DynAgentLogic agentLogic;

    private Id id;

    private MobsimVehicle veh;

    private InternalInterface internalInterface;

    private EventsManager eventsManager;

    private MobsimAgent.State state;

    // =====

    private DynLeg dynLeg;

    private Id currentLinkId;

    // =====

    private DynActivity dynActivity;

    private double activityEndTime = Time.UNDEFINED_TIME;


    // =====

    public DynAgent(Id id, Id startLinkId, InternalInterface internalInterface,
            DynAgentLogic agentLogic)
    {
        this.id = id;
        this.currentLinkId = startLinkId;
        this.agentLogic = agentLogic;
        this.internalInterface = internalInterface;
        this.eventsManager = internalInterface.getMobsim().getEventsManager();

        // initial activity
        dynActivity = this.agentLogic.init(this);
        activityEndTime = dynActivity.getEndTime();

        if (activityEndTime != Time.UNDEFINED_TIME) {
            state = MobsimAgent.State.ACTIVITY;
        }
        else {
            state = MobsimAgent.State.ABORT;// ??????
        }
    }


    public void update()
    {
        if (state == null) {
            // this agent is right now switching from one task (Act/Leg) to another (Act/Leg)
            // so he is the source of this schedule updating process and so he will not be handled here
            // TODO: verify this condition!!!
            // TODO: should this condition be moved to AgentLogic?
            return;
        }

        switch (state) {
            case ACTIVITY: // WAIT (will it be also SERVE???)
                if (activityEndTime != dynActivity.getEndTime()) {
                    activityEndTime = dynActivity.getEndTime();
                    internalInterface.rescheduleActivityEnd(this);
                }
                break;

            case LEG: // DRIVE
                // currently not supported (only if VEHICLE DIVERSION is turned ON)
                // but in general, this should be handled by vrpLeg itself!

                // idea: pass destionationLinkId and linkIds to the vrpLeg...
                break;

            default:
                throw new IllegalStateException();
        }
    }


    private void computeNextAction(DynAction oldDynAction, double now)
    {
        state = null;// !!! this is important
        dynActivity = null;
        dynLeg = null;

        DynAction nextDynAction = agentLogic.computeNextAction(oldDynAction, now);

        if (nextDynAction instanceof DynActivity) {
            dynActivity = (DynActivity)nextDynAction;
            activityEndTime = dynActivity.getEndTime();
            state = MobsimAgent.State.ACTIVITY;

            eventsManager.processEvent(eventsManager.getFactory().createActivityStartEvent(now, id,
                    currentLinkId, null, dynActivity.getActivityType()));
        }
        else {
            dynLeg = (DynLeg)nextDynAction;
            state = MobsimAgent.State.LEG;
        }
    }


    @Override
    public void endActivityAndComputeNextState(double now)
    {
        eventsManager.processEvent(eventsManager.getFactory().createActivityEndEvent(now, id,
                currentLinkId, null, dynActivity.getActivityType()));

        computeNextAction(dynActivity, now);
    }


    @Override
    public void endLegAndComputeNextState(double now)
    {
        eventsManager.processEvent(eventsManager.getFactory().createAgentArrivalEvent(now, id,
                currentLinkId, TransportMode.car));

        computeNextAction(dynLeg, now);
    }


    @Override
    public void abort(double now)
    {
        this.state = MobsimAgent.State.ABORT;
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
        return (state == State.LEG) ? TransportMode.car : null;
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
        double now = internalInterface.getMobsim().getSimTimer().getTimeOfDay();
        dynLeg.movedOverNode(currentLinkId, newLinkId, (int)now);
        currentLinkId = newLinkId;
    }


    @Override
    public double getActivityEndTime()
    {
        return activityEndTime;
    }


    @Override
    public void notifyArrivalOnLinkByNonNetworkMode(Id linkId)
    {
        throw new UnsupportedOperationException(
                "This is used only for teleportation and this agent does not teleport");
    }


    @Override
    public Double getExpectedTravelTime()
    {
        throw new UnsupportedOperationException(
                "This is used only for teleportation and this agent does not teleport");
    }
}
