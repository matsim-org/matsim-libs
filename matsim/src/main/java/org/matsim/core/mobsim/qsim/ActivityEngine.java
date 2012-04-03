package org.matsim.core.mobsim.qsim;

import java.util.Collection;
import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.comparators.PlanAgentDepartureTimeComparator;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.pt.AbstractTransitDriver;
import org.matsim.core.mobsim.qsim.pt.TransitDriver;
import org.matsim.core.utils.misc.Time;

public class ActivityEngine implements MobsimEngine {
	/**
	 * This list needs to be a "blocking" queue since this is needed for
	 * thread-safety in the parallel qsim. cdobler, oct'10
	 */
	private Queue<MobsimAgent> activityEndsList = new PriorityBlockingQueue<MobsimAgent>(500, new PlanAgentDepartureTimeComparator());

	private InternalInterface internalInterface;

	/**
	 * Registers this agent as performing an activity and makes sure that the
	 * agent will be informed once his departure time has come.
	 * @param agent
	 * 
	 * @see MobsimDriverAgent#getActivityEndTime()
	 */
	void arrangeActivityStart(final MobsimAgent agent) {
		activityEndsList.add(agent);
		if (!(agent instanceof AbstractTransitDriver)) {
			// yy why?  kai, mar'12
			
			internalInterface.registerAdditionalAgentOnLink(agent);
		}
		if ( agent.getActivityEndTime()==Double.POSITIVE_INFINITY ) {
			internalInterface.getMobsim().getAgentCounter().decLiving() ;
		}
	}

	void rescheduleActivityEnd(final MobsimAgent agent, final double oldTime, final double newTime ) {
		// yyyy possibly, this should be "notifyChangedPlan".  kai, oct'10
		// yy the "newTime" is strictly speaking not necessary.  It is there so people do not put in the 
		// new time instead of the old time, since then it will not work.  kai, oct'10
	
		// remove agent from queue
		activityEndsList.remove(agent);
		
		// The intention in the following is that an agent that is no longer alive has an activity end time of infinity.  The number of
		// alive agents is only modified when an activity end time is changed between a finite time and infinite.  kai, jun'11
		/*
		 * If an agent performs only a single iteration, the old departure time is Time.UNDEFINED which
		 * is Double.NEGATIVE_INFINITY. If an agent performs the last of several activities, the old
		 * departure time is Double.POSITIVE_INFINITY.
		 * If an agent is (re)activated, it is also (un)registered at an activity location. cdobler, oct'11
		 */
		if (oldTime == Double.POSITIVE_INFINITY || oldTime == Time.UNDEFINED_TIME) {
			if (newTime == Double.POSITIVE_INFINITY) {
				// agent was de-activated and still should be de-activated - nothing to do here
			} else {
				// newTime != Double.POSITIVE_INFINITY - re-activate the agent
				activityEndsList.add(agent);
				internalInterface.registerAdditionalAgentOnLink(agent);
				((AgentCounter) internalInterface.getMobsim().getAgentCounter()).incLiving();				
			}
		} 
		/*
		 * After the re-planning the agent's current activity has changed to its last activity.
		 * Therefore the agent is de-activated. cdobler, oct'11
		 */
		else if (newTime == Double.POSITIVE_INFINITY) {
			unregisterAgentAtActivityLocation(agent);
			internalInterface.getMobsim().getAgentCounter().decLiving();
		} 
		/*
		 *  The activity is just rescheduled during the day, so we keep the agent active. cdobler, oct'11
		 */
		else {
			activityEndsList.add(agent);
		}
	}

	private void unregisterAgentAtActivityLocation(final MobsimAgent agent) {
		if (!(agent instanceof TransitDriver)) {
			Id agentId = agent.getId();
			Id linkId = agent.getCurrentLinkId();
			internalInterface.unregisterAdditionalAgentOnLink(agentId, linkId);
		}
	}

	Double getNextActivityEndTime() {
		MobsimAgent firstAgent = activityEndsList.peek();
		if (firstAgent != null) {
			double nextActivityEndTime = firstAgent.getActivityEndTime();
			return nextActivityEndTime;
		} else {
			return null;
		}
		
	}

	Collection<MobsimAgent> getActivityEndsList() {
		return Collections.unmodifiableCollection(activityEndsList);
	}

	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}

	@Override
	public void doSimStep(double time) {
		while (activityEndsList.peek() != null) {
			MobsimAgent agent = activityEndsList.peek();
			if (agent.getActivityEndTime() <= time) {
				activityEndsList.poll();
				unregisterAgentAtActivityLocation(agent);
				agent.endActivityAndComputeNextState(time);
				internalInterface.arrangeNextAgentState(agent) ;
			} else {
				return;
			}
		}
	}

	@Override
	public Netsim getMobsim() {
		return internalInterface.getMobsim();
	}

	@Override
	public void onPrepareSim() {
		// Nothing to do here
	}

	@Override
	public void afterSim() {
		double now = this.internalInterface.getMobsim().getSimTimer().getTimeOfDay();
		for (MobsimAgent agent : activityEndsList) {
			if ( agent.getActivityEndTime()!=Double.POSITIVE_INFINITY 
					&& agent.getActivityEndTime()!=Time.UNDEFINED_TIME ) {
		
				// since we are at an activity, it is not plausible to assume that the agents know mode or destination 
				// link id.  Thus generating the event with ``null'' in the corresponding entries.  kai, mar'12
				EventsManager eventsManager = internalInterface.getMobsim().getEventsManager();
				eventsManager.processEvent(eventsManager.getFactory().createAgentStuckEvent(now, agent.getId(),null, null));
		
			}
		}
		activityEndsList.clear();
	}
	
}