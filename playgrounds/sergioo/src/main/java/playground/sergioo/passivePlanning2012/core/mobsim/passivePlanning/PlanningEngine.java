package playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

import playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.agents.PassivePlannerDriverAgent;
import playground.sergioo.passivePlanning2012.population.parallelPassivePlanning.PassivePlannerManager;

public class PlanningEngine implements MobsimEngine, DepartureHandler {

	private List<PassivePlannerDriverAgent> planningAgents = new LinkedList<PassivePlannerDriverAgent>();
	private InternalInterface internalInterface;
	private PassivePlannerManager passivePlannerManager;
	
	public PlanningEngine(PassivePlannerManager passivePlannerManager) {
		this.passivePlannerManager = passivePlannerManager;
	}
	@Override
	public void doSimStep(double time) {
		passivePlannerManager.setTime(time);
		for(int i=0; i<planningAgents.size(); i++)
			if(planningAgents.get(i).isPlanned()) {
				PassivePlannerDriverAgent planningAgent = planningAgents.remove(i);
				internalInterface.arrangeNextAgentState(planningAgent);
			}
	}
	@Override
	public boolean handleDeparture(double now, MobsimAgent agent, Id linkId) {
		if(agent instanceof PassivePlannerDriverAgent && !((PassivePlannerDriverAgent)agent).isPlanned()) {
			planningAgents.add((PassivePlannerDriverAgent) agent);
			return true;
		}
		return false;
	}
	@Override
	public void onPrepareSim() {
		
	}
	@Override
	public void afterSim() {
		double now = internalInterface.getMobsim().getSimTimer().getTimeOfDay();
		for (PassivePlannerDriverAgent planningAgent:planningAgents) {
			EventsManager eventsManager = internalInterface.getMobsim()
					.getEventsManager();
			eventsManager.processEvent(eventsManager.getFactory()
					.createAgentStuckEvent(now, planningAgent.getId(),
							planningAgent.getDestinationLinkId(), "Planning"));
		}
		planningAgents.clear();
	}
	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}

}
