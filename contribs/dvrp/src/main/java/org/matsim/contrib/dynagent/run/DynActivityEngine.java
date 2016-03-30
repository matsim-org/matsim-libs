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

package org.matsim.contrib.dynagent.run;

import java.util.*;

import javax.inject.Inject;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimAgent.State;
import org.matsim.core.mobsim.qsim.*;


/**
 * It might be nicer to have ActivityEngine as a delegate, not as the superclass. But there is a
 * hardcoded "instanceof ActivityEngine" check in QSim :-( TODO introduce an ActivityEngine
 * interface?
 * 
 * DynActivityEngine and ActivityEngine could be decoupled
 * (if we can ensure DynActivityEngine's handleActivity() is called before that of ActivityEngine)
 */
public class DynActivityEngine
    extends ActivityEngine
{
    private InternalInterface internalInterface;

    private final List<DynAgent> dynAgents = new LinkedList<>();
    private final List<DynAgent> newDynAgents = new ArrayList<>();//will to be handled in the next timeStep


    @Inject
    public DynActivityEngine(EventsManager eventsManager)
    {
        super(eventsManager);
    }


    // See handleActivity for the reason for this.
    private boolean beforeFirstSimStep = true;


    @Override
    public void doSimStep(double time)
    {
        beforeFirstSimStep = false;
        dynAgents.addAll(newDynAgents);
        newDynAgents.clear();

        Iterator<DynAgent> dynAgentIter = dynAgents.iterator();
        while (dynAgentIter.hasNext()) {
            DynAgent agent = dynAgentIter.next();
            if (agent.getState() == State.ACTIVITY) {
                agent.doSimStep(time);
                //ask agents about the current activity end time;
                double currentEndTime = agent.getActivityEndTime();

                if (currentEndTime == Double.POSITIVE_INFINITY) { //agent says: stop simulating me
                    unregisterAgentAtActivityLocation(agent);
                    internalInterface.getMobsim().getAgentCounter().decLiving();
                    dynAgentIter.remove();
                }
                else if (currentEndTime <= time) { //the agent wants to end the activity NOW
                    unregisterAgentAtActivityLocation(agent);
                    agent.endActivityAndComputeNextState(time);
                    internalInterface.arrangeNextAgentState(agent);
                    dynAgentIter.remove();
                }
            }
        }

        super.doSimStep(time);
    }


    @Override
    public boolean handleActivity(MobsimAgent agent)
    {
        if (! (agent instanceof DynAgent)) {
            return super.handleActivity(agent);
        }

        double endTime = agent.getActivityEndTime();
        double currentTime = internalInterface.getMobsim().getSimTimer().getTimeOfDay();

        if (endTime == Double.POSITIVE_INFINITY) {
            // This is the last planned activity.
            // So the agent goes to sleep.
            internalInterface.getMobsim().getAgentCounter().decLiving();
        }
        else if (endTime <= currentTime && !beforeFirstSimStep) {
            // This activity is already over (planned for 0 duration)
            // So we proceed immediately.
            agent.endActivityAndComputeNextState(currentTime);
            internalInterface.arrangeNextAgentState(agent);
        }
        else {
            // The agent commences an activity on this link.
            if (beforeFirstSimStep) {
                dynAgents.add((DynAgent)agent);
            }
            else {
                newDynAgents.add((DynAgent)agent);
            }

            internalInterface.registerAdditionalAgentOnLink(agent);
        }

        return true;
    }


    @Override
    public void afterSim()
    {
        super.afterSim();
        dynAgents.clear();
    }


    @Override
    public void setInternalInterface(InternalInterface internalInterface)
    {
        this.internalInterface = internalInterface;
        super.setInternalInterface(internalInterface);
    }


    private void unregisterAgentAtActivityLocation(final MobsimAgent agent)
    {
        Id<Person> agentId = agent.getId();
        Id<Link> linkId = agent.getCurrentLinkId();
        if (linkId != null) { // may be bushwacking
            internalInterface.unregisterAdditionalAgentOnLink(agentId, linkId);
        }
    }
}
