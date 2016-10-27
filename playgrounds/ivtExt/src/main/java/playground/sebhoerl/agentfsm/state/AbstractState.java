package playground.sebhoerl.agentfsm.state;

import playground.sebhoerl.agentfsm.agent.FSMAgent;
import playground.sebhoerl.agentfsm.instruction.AdvanceInstruction;
import playground.sebhoerl.agentfsm.instruction.Instruction;

/**
 * Convenience class for using the FSM states. Instead of method arguments all the relevant
 * structures can be accessed as member variables in derived state classes.
 */
public abstract class AbstractState implements FSMState {
	protected FSMAgent agent;
	protected double now;
	
	private void prepareState(double now, FSMAgent agent) {
		this.now = now;
		this.agent = agent;
	}
	
	@Override
	public Instruction enter(double now, FSMAgent agent) {
		prepareState(now, agent);
		return enter();
	}

	@Override
	public AdvanceInstruction leave(double now, FSMAgent agent) {
		prepareState(now, agent);
		return leave();
	}

	abstract protected Instruction enter();
	abstract protected AdvanceInstruction leave();
}
