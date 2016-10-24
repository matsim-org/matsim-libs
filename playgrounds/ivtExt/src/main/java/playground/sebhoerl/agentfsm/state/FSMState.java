package playground.sebhoerl.agentfsm.state;

import playground.sebhoerl.agentfsm.agent.FSMAgent;
import playground.sebhoerl.agentfsm.instruction.AdvanceInstruction;
import playground.sebhoerl.agentfsm.instruction.Instruction;

public interface FSMState {
	public Instruction enter(double now, FSMAgent agent);
	public AdvanceInstruction leave(double now, FSMAgent agent);
}
