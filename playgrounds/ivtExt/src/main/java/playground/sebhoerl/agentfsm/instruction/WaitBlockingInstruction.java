package playground.sebhoerl.agentfsm.instruction;

import org.matsim.core.mobsim.framework.MobsimAgent.State;

import playground.sebhoerl.agentlock.lock.AgentLock;
import playground.sebhoerl.agentlock.lock.LockHandle;

public class WaitBlockingInstruction implements LockInstruction {
    @Override
    public LockHandle createLockHandle(AgentLock lock, State state) {
        return lock.acquireBlocking(state);
    }
}
