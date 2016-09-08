package playground.sebhoerl.agentlock.lock;

import org.matsim.core.mobsim.framework.MobsimAgent;

import playground.sebhoerl.agentlock.agent.LockAgent;

public interface LockHandle {
    public LockAgent getAgent();
    public boolean isValid();
    public MobsimAgent.State getState();
    public double getEndTime();
    public AgentLock.Type getLockType();
}
