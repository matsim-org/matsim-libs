package playground.sebhoerl.agentfsm.instruction;

import org.matsim.core.mobsim.framework.MobsimAgent.State;

import playground.sebhoerl.agentlock.lock.AgentLock;
import playground.sebhoerl.agentlock.lock.LockHandle;

public interface LockInstruction extends Instruction {
    LockHandle createLockHandle(AgentLock lock, State state);
}
