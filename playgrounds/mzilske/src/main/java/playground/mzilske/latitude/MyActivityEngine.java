package playground.mzilske.latitude;

import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.comparators.PlanAgentDepartureTimeComparator;
import org.matsim.core.mobsim.qsim.interfaces.ActivityHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.pt.TransitDriver;
import org.matsim.core.utils.misc.Time;

public class MyActivityEngine implements MobsimEngine, ActivityHandler {
	
	private Queue<MobsimAgent> activityEndsList = new PriorityBlockingQueue<MobsimAgent>(500, new PlanAgentDepartureTimeComparator());

	private InternalInterface internalInterface;
	
	private void unregisterAgentAtActivityLocation(final MobsimAgent agent) {
		if (!(agent instanceof TransitDriver)) {
			Id agentId = agent.getId();
			Id linkId = agent.getCurrentLinkId();
			internalInterface.unregisterAdditionalAgentOnLink(agentId, linkId);
		}
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


	@Override
	public boolean handleActivity(MobsimAgent agent) {
		activityEndsList.add(agent);
		if ( agent.getActivityEndTime()==Double.POSITIVE_INFINITY ) {
			internalInterface.getMobsim().getAgentCounter().decLiving() ;
		}
		return true;
	}
	
}