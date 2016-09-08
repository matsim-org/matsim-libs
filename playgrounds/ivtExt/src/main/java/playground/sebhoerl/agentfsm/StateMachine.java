package playground.sebhoerl.agentfsm;

import java.util.HashMap;
import java.util.Map;

import playground.sebhoerl.agentfsm.agent.FSMAgent;
import playground.sebhoerl.agentfsm.instruction.AdvanceInstruction;
import playground.sebhoerl.agentfsm.instruction.Instruction;
import playground.sebhoerl.agentfsm.state.ActivityState;
import playground.sebhoerl.agentfsm.state.FSMState;
import playground.sebhoerl.agentfsm.state.LegState;

public class StateMachine {
	final private Map<String, FSMState> states
		= new HashMap<String, FSMState>();
	
	public void addState(String stateId, FSMState state) {
	    if (!(state instanceof ActivityState) && !(state instanceof LegState)) {
	        throw new IllegalArgumentException(String.format("State %s must be either ActivityState or LegState", state.getClass().toString()));
	    }
	    
		states.put(stateId, state);
	}
	
	public FSMState getState(String stateId) {
	    if (!states.containsKey(stateId)) {
	        throw new IllegalStateException(String.format("The FSM state '%s' does not exist!", stateId));
	    }
	    
		return states.get(stateId);
	}
	
	public void setInitialStateId(final String initialStateId) {
	    states.put("__INITIAL__", new FSMState() {
            @Override
            public Instruction enter(double now, FSMAgent agent) {
                throw new IllegalStateException();
            }

            @Override
            public AdvanceInstruction leave(double now, FSMAgent agent) {
                return new AdvanceInstruction(initialStateId);
            }
	    });
	}
}
