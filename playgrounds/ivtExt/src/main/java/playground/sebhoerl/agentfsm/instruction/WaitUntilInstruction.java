package playground.sebhoerl.agentfsm.instruction;

import org.matsim.core.mobsim.framework.MobsimAgent.State;

import playground.sebhoerl.agentlock.lock.AgentLock;
import playground.sebhoerl.agentlock.lock.LockHandle;

public class WaitUntilInstruction implements LockInstruction {
    final private double time;
    
    public WaitUntilInstruction(double time) {
        this.time = time;
    }
    
    public LockHandle createLockHandle(AgentLock lock, State state) {
        return lock.acquireUntil(state, time);
    }
    
    public double getTime() {
        return time;
    }
}
