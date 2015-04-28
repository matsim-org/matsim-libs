package playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning;

import java.util.Comparator;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.ActivityHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.utils.misc.Time;

public class PassivePlanningActivityEngine implements MobsimEngine, ActivityHandler {

	/**
	 * Agents cannot be added directly to the activityEndsList since that would
	 * not be thread-safe when within-day replanning is used. There, an agent's 
	 * activity end time can be modified. As a result, the agent is located at
	 * the wrong position in the activityEndsList until it is updated by using
	 * rescheduleActivityEnd(...). However, if another agent is added to the list
	 * in the mean time, it might be inserted at the wrong position.
	 * cdobler, apr'12
	 */
	public static class AgentEntry {
		public MobsimAgent agent;
		public double activityEndTime;
		public AgentEntry(MobsimAgent agent, double activityEndTime) {
			this.agent = agent;
			this.activityEndTime = activityEndTime;
		}
	}

	/**
	 * This list needs to be a "blocking" queue since this is needed for
	 * thread-safety in the parallel qsim. cdobler, oct'10
	 */
	public static Queue<AgentEntry> activityEndsList = new PriorityBlockingQueue<AgentEntry>(500, new Comparator<AgentEntry>() {

		@Override
		public int compare(AgentEntry arg0, AgentEntry arg1) {
			int cmp = Double.compare(arg0.activityEndTime, arg1.activityEndTime);
			if (cmp == 0) {
				// Both depart at the same time -> let the one with the larger id be first (=smaller)
				//
				// yy We are not sure what the above comment line is supposed to say.  Presumably, it is supposed
				// to say that the agent with the larger ID should be "smaller" one in the comparison. 
				// In practice, it seems
				// that something like "emob_9" is before "emob_8", and something like "emob_10" before "emob_1".
				// It is unclear why this convention is supposed to be helpful.
				// kai & dominik, jul'12
				//
				return arg1.agent.getId().compareTo(arg0.agent.getId());
			}
			return cmp;
		}

	});

	private InternalInterface internalInterface;

	@Override
	public void onPrepareSim() {
		// Nothing to do here
	}

	@Override
	public void doSimStep(double time) {
		while (activityEndsList.peek() != null) {
			MobsimAgent agent = activityEndsList.peek().agent;
			if (activityEndsList.peek().activityEndTime <= time) {
				activityEndsList.poll();
				unregisterAgentAtActivityLocation(agent);
				agent.endActivityAndComputeNextState(time);
				internalInterface.arrangeNextAgentState(agent);
			} else {
				return;
			}
		}
	}

	@Override
	public void afterSim() {
		double now = this.internalInterface.getMobsim().getSimTimer().getTimeOfDay();
		for (AgentEntry entry : activityEndsList) {
			if (entry.activityEndTime!=Double.POSITIVE_INFINITY && entry.activityEndTime!=Time.UNDEFINED_TIME) {
				// since we are at an activity, it is not plausible to assume that the agents know mode or destination 
				// link id.  Thus generating the event with ``null'' in the corresponding entries.  kai, mar'12
				EventsManager eventsManager = ((QSim) internalInterface.getMobsim()).getEventsManager();
				eventsManager.processEvent(new PersonStuckEvent(now, entry.agent.getId(), null, null));
			}
		}
		activityEndsList.clear();
	}

	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}

	@Override
	public boolean handleActivity(MobsimAgent agent) {
		/*
		 * Add the agent to the activityEndsList if it is not its last
		 * activity and register it on the link. Otherwise decrease the
		 * agents counter by one.
		 */
		if (agent.getActivityEndTime() == Double.POSITIVE_INFINITY) {
			((QSim) internalInterface.getMobsim()).getAgentCounter().decLiving();
		} else if (agent.getActivityEndTime() <= internalInterface.getMobsim().getSimTimer().getTimeOfDay()) {
			// This activity is already over (planned for 0 duration)
			// So we proceed immediately.
			agent.endActivityAndComputeNextState(internalInterface.getMobsim().getSimTimer().getTimeOfDay());
			internalInterface.arrangeNextAgentState(agent);
		} else {
			activityEndsList.add(new AgentEntry(agent, agent.getActivityEndTime()));
			internalInterface.registerAdditionalAgentOnLink(agent);			
		}
		return true;
	}

	private void unregisterAgentAtActivityLocation(final MobsimAgent agent) {
		Id<Person> agentId = agent.getId();
		Id<Link> linkId = agent.getCurrentLinkId();
		if (linkId != null) { // may be bushwacking
			internalInterface.unregisterAdditionalAgentOnLink(agentId, linkId);
		}
	}

}
