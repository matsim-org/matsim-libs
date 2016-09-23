package playground.sebhoerl.agentlock.lock;

import org.matsim.core.mobsim.framework.MobsimAgent;

import playground.sebhoerl.agentlock.agent.LockAgent;

public class LockHandleImpl implements LockHandle { 
    final private LockAgent agent;
    final private MobsimAgent.State state;
    final private double endTime;
    final private AgentLock.Type lockType;
    final private long lockIndex;
    final AgentLock lock;
    
    public LockHandleImpl(LockAgent agent, AgentLock lock, MobsimAgent.State state, double endTime) {
        this.agent = agent;
        this.state = state;
        this.endTime = endTime;
        this.lockType = lock.getType();
        this.lockIndex = lock.getIndex();
        this.lock = lock;
    }
    
    public LockAgent getAgent() {
        return agent;
    }
    
    public boolean isValid() {
        return lockIndex == lock.getIndex();
    }
    
    public MobsimAgent.State getState() {
        return state;
    }
    
    public double getEndTime() {
        return endTime;
    }
    
    public AgentLock.Type getLockType() {
        return lockType;
    }
}
