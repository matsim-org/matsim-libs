package playground.sebhoerl.agentlock.agent;

import org.matsim.core.mobsim.framework.MobsimAgent;

import playground.sebhoerl.agentlock.lock.LockHandle;

public interface LockAgent extends MobsimAgent {
    LockHandle computeNextState(double now);
    
    void changeStateToLeg();
    void requestEndLeg();
}
