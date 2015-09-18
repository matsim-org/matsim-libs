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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimAgent.State;
import org.matsim.core.mobsim.qsim.*;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.utils.misc.Time;


/**
 * It might be nicer to have ActivityEngine as a delegate, not as the superclass. But there is a
 * hardcoded "instanceof ActivityEngine" check in QSim :-( TODO introduce an ActivityEngine
 * interface?
 */
public class DynActivityEngine
    extends ActivityEngine
{
    private static class EndTimeEntry
    {
        public final DynAgent agent;
        public double scheduledEndTime = Time.UNDEFINED_TIME;


        public EndTimeEntry(DynAgent agent)
        {
            this.agent = agent;
        }
    }


    private final Map<Id<Person>, EndTimeEntry> activityEndTimes;

    private InternalInterface internalInterface;


    public DynActivityEngine(EventsManager eventsManager, AgentCounter agentCounter)
    {
        super(eventsManager, agentCounter);
        activityEndTimes = new HashMap<>();
    }


    @Override
    public void doSimStep(double time)
    {
        for (EndTimeEntry e : activityEndTimes.values()) {
            if (e.agent.getState() == State.ACTIVITY) {
                e.agent.doSimStep(time);

                //ask agents who are performing an activity about its end time;
                double currentEndTime = e.agent.getActivityEndTime();

                if (e.scheduledEndTime != currentEndTime) {
                    //we may reschedule the agent right now, but rescheduling is quite costly
                    //i.e. it requires iteration through all agents (and there may be millions of them)
                    //and if the agent is very indecisive we may repeat this operation each sim step
                    //that is why it is better to defer the rescheduling as much as possible

                    if (currentEndTime <= time //the agent wants to end the activity NOW, or
                            || e.scheduledEndTime <= time) { //the simulation thinks the agent wants to finish the activity NOW
                        internalInterface.rescheduleActivityEnd(e.agent);
                        e.scheduledEndTime = currentEndTime;
                    }
                }
            }
        }

        super.doSimStep(time);
    }


    @Override
    public boolean handleActivity(MobsimAgent agent)
    {
        if (super.handleActivity(agent)) {
            if (agent instanceof DynAgent) {

                //0-second activities are ended within super.handleActivity(agent)
                //(even without DynActivity.doSimStep(now))
                //so the state may be different (i.e. LEG)
                if (agent.getState() == State.ACTIVITY) {
                    EndTimeEntry entry = activityEndTimes.get(agent.getId());

                    if (entry == null) {
                        entry = new EndTimeEntry((DynAgent)agent);
                        activityEndTimes.put(agent.getId(), entry);
                    }

                    entry.scheduledEndTime = agent.getActivityEndTime();
                }
            }

            return true;
        }

        return false;
    }


    @Override
    public void afterSim()
    {
        super.afterSim();
        activityEndTimes.clear();
    }


    @Override
    public void setInternalInterface(InternalInterface internalInterface)
    {
        this.internalInterface = internalInterface;
        super.setInternalInterface(internalInterface);
    }
}
