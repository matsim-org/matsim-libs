/* *********************************************************************** *
 * project: org.matsim.*
 * Sim2DActivityEngine.java
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

package playground.gregor.sim2d_v3.simulation;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.ConcurrentSkipListSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.ActivityHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.utils.misc.Time;

/**
 * Adding sub-time step departure resolution for Sim2D. To do so, we use
 * a ConcurrentSkipListSet instead of a PriorityQueue because we can iterate over it.
 * 
 * @author cdobler
 */
public class Sim2DActivityEngine implements MobsimEngine, ActivityHandler {

	private static final Logger log = Logger.getLogger(Sim2DActivityEngine.class);
	
	private InternalInterface internalInterface;
	
	/**
	 * This list needs to be a thread-safe since this is needed for
	 * thread-safety in the parallel qsim. cdobler, oct'10
	 */
	private Collection<AgentEntry> activityEndsList = new ConcurrentSkipListSet<AgentEntry>(new Comparator<AgentEntry>() {

		@Override
		public int compare(AgentEntry arg0, AgentEntry arg1) {
			int cmp = Double.compare(arg0.activityEndTime, arg1.activityEndTime);
			if (cmp == 0) {
				// Both depart at the same time -> let the one with the larger id be first (=smaller)
				return arg1.agent.getId().compareTo(arg0.agent.getId());
			}
			return cmp;
		}
	});
	
	public Sim2DActivityEngine() {
	}
	
	@Override
	public void doSimStep(double time) {
		Iterator<AgentEntry> iter = activityEndsList.iterator();
		
		while (iter.hasNext()) {
			AgentEntry entry = iter.next();
			MobsimAgent agent = entry.agent;
			
			/*
			 * For agents that should depart until the current time step.
			 */
			if (agent.getActivityEndTime() <= time) {
				
				/* 
				 * Shift the value by one because if an agent arrives later at an activity
				 * the the scheduled activity end time, the agent will set the activity end time
				 * to the arrival time but will still perform the activity for one second. Therefore
				 * we add one second to ignore those agents.
				 */
				if (agent.getActivityEndTime() + 1 < time) {
					log.warn("Agent missed its departure time?! Agent:" + agent.getId().toString() +
							", time: " + time + ", departure time: " + agent.getActivityEndTime());  
				}
								
				iter.remove();
				unregisterAgentAtActivityLocation(agent);
				agent.endActivityAndComputeNextState(time);
				internalInterface.arrangeNextAgentState(agent);
			} 
			/*
			 * For agents that should depart within the current time step.
			 * If they perform a walk2d leg, we also let them depart.
			 */
			else if (agent.getActivityEndTime() < time + 1.0) {
				if (agent instanceof PlanAgent) {
					PlanAgent planAgent = (PlanAgent) agent;
					PlanElement planElement = planAgent.getNextPlanElement();
					if (planElement instanceof Leg) {
						Leg leg = (Leg) planElement;
						if (leg.getMode().equals("walk2d")) {
							iter.remove();
							unregisterAgentAtActivityLocation(agent);
							agent.endActivityAndComputeNextState(time);
							internalInterface.arrangeNextAgentState(agent);
						}
					}
				}
			} else {
				return;
			}
		}
	}
	
	private void unregisterAgentAtActivityLocation(final MobsimAgent agent) {
		Id agentId = agent.getId();
		Id linkId = agent.getCurrentLinkId();
		internalInterface.unregisterAdditionalAgentOnLink(agentId, linkId);
	}

	@Override
	public boolean handleActivity(MobsimAgent agent) {
		if (agent.getActivityEndTime() == Double.POSITIVE_INFINITY) {
			internalInterface.getMobsim().getAgentCounter().decLiving();
		} else {
			activityEndsList.add(new AgentEntry(agent, agent.getActivityEndTime()));
			internalInterface.registerAdditionalAgentOnLink(agent);			
		}
		return true;
	}

	@Override
	public void onPrepareSim() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterSim() {
		double now = this.internalInterface.getMobsim().getSimTimer().getTimeOfDay();
		for (AgentEntry entry : activityEndsList) {
			if (entry.activityEndTime!=Double.POSITIVE_INFINITY && entry.activityEndTime!=Time.UNDEFINED_TIME) {
				// since we are at an activity, it is not plausible to assume that the agents know mode or destination 
				// link id.  Thus generating the event with ``null'' in the corresponding entries.  kai, mar'12
				EventsManager eventsManager = internalInterface.getMobsim().getEventsManager();
				eventsManager.processEvent(eventsManager.getFactory().createAgentStuckEvent(now, entry.agent.getId(),null, null));
			}
		}
		activityEndsList.clear();
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}
	
	private class AgentEntry {
		public AgentEntry(MobsimAgent agent, double activityEndTime) {
			this.agent = agent;
			this.activityEndTime = activityEndTime;
		}
		MobsimAgent agent;
		double activityEndTime;
	}

}
