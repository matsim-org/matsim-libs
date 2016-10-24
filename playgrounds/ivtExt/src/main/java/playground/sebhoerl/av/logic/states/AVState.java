package playground.sebhoerl.av.logic.states;

import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.EventsManager;

import playground.sebhoerl.agentfsm.agent.FSMAgent;
import playground.sebhoerl.agentfsm.instruction.AdvanceInstruction;
import playground.sebhoerl.agentfsm.instruction.Instruction;
import playground.sebhoerl.agentfsm.state.AbstractState;
import playground.sebhoerl.av.logic.agent.AVAgent;
import playground.sebhoerl.av.logic.service.Service;

abstract public class AVState extends AbstractState {	
	private EventsManager events;
	protected Service service;
	
	public void setEventManager(EventsManager events) {
		this.events = events;
	}
	
	@Override
	public Instruction enter(double now, FSMAgent agent) {
        service = ((AVAgent)agent).getService();
        return super.enter(now, agent);
	}

	@Override
	public AdvanceInstruction leave(double now, FSMAgent agent) {
        service = ((AVAgent)agent).getService();
        return super.leave(now, agent);
	}
	
	protected void postActivityStartEvent(String event) {
		events.processEvent(new ActivityStartEvent(now, agent.getId(), agent.getCurrentLinkId(), null, event));
	}
	
	protected void postActivityEndEvent(String event) {
		events.processEvent(new ActivityEndEvent(now, agent.getId(), agent.getCurrentLinkId(), null, event));
	}
}
