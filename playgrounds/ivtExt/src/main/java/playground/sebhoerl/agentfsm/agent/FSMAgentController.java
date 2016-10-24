package playground.sebhoerl.agentfsm.agent;

import org.matsim.core.mobsim.framework.MobsimAgent.State;

import playground.sebhoerl.agentfsm.StateMachine;
import playground.sebhoerl.agentfsm.instruction.AdvanceInstruction;
import playground.sebhoerl.agentfsm.instruction.Instruction;
import playground.sebhoerl.agentfsm.instruction.LockInstruction;
import playground.sebhoerl.agentfsm.state.FSMState;
import playground.sebhoerl.agentfsm.state.LegState;
import playground.sebhoerl.agentlock.lock.AgentLock;
import playground.sebhoerl.agentlock.lock.LockHandle;

public class FSMAgentController {
	private FSMAgent agent;
	private AgentLock agentLock;
	
	private String currentStateId = "__INITIAL__";
	private StateMachine machine;
	
	public FSMAgentController(FSMAgent agent, AgentLock agentLock, StateMachine machine) {
		this.agent = agent;
		this.agentLock = agentLock;
		this.machine = machine;
	}
	
	public String getCurrentStateId() {
		return currentStateId;
	}
	
	static private Instruction enter(FSMState state, double now, FSMAgent agent) {
	    Instruction instruction = state.enter(now, agent);
	    
	    if (instruction == null) {
	        throw new RuntimeException(String.format("FSMState %s.enter return no instruction", state.getClass().toString()));
	    }
	    
	    return instruction;
	}
	
    static private Instruction leave(FSMState state, double now, FSMAgent agent) {
        Instruction instruction = state.leave(now, agent);
        
        if (instruction == null) {
            throw new RuntimeException(String.format("FSMState %s.leave return no instruction", state.getClass().toString()));
        }
        
        return instruction;
    }
	
    public LockHandle computeNextState(double now) {
        FSMState currentState = machine.getState(currentStateId);
        Instruction nextInstruction = leave(currentState, now, agent);
    	
    	do {
    	    String nextStateId = ((AdvanceInstruction) nextInstruction).getDestinationStateId();
    		
    	    currentState = machine.getState(nextStateId);
    		currentStateId = nextStateId;
    		
    		nextInstruction = enter(currentState, now, agent);
    	} while (nextInstruction instanceof AdvanceInstruction);

    	if (currentState instanceof LegState) {
    		agent.startLeg(((LegState) currentState).createLeg());
    	}

    	if (nextInstruction instanceof LockInstruction) {
    	    return ((LockInstruction) nextInstruction).createLockHandle(
    	            agentLock, (currentState instanceof LegState) ? State.LEG : State.ACTIVITY);
    	} else {
    	    throw new RuntimeException(String.format("FSMState %s must either return a LockInstruction or AdvanceInstruction", currentState.getClass().toString()));
    	}
    }
    
    public void release() {
        agentLock.release();
    }
}
