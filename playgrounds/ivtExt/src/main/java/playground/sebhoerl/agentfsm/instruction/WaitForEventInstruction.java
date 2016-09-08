package playground.sebhoerl.agentfsm.instruction;

import org.matsim.core.mobsim.framework.MobsimAgent.State;

import playground.sebhoerl.agentlock.events.Event;
import playground.sebhoerl.agentlock.lock.AgentLock;
import playground.sebhoerl.agentlock.lock.LockHandle;

public class WaitForEventInstruction implements LockInstruction {
    final private Event event;
    
    public WaitForEventInstruction(Event event) {
        this.event = event;
    }
    
    @Override
    public LockHandle createLockHandle(AgentLock lock, State state) {
        return lock.acquireEvent(state, event);
    }
    
    public Event getEvent() {
        return this.event;
    }
}
