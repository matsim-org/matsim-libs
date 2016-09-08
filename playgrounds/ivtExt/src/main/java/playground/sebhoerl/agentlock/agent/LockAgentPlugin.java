package playground.sebhoerl.agentlock.agent;

import org.matsim.core.mobsim.framework.MobsimAgent.State;
import org.matsim.core.utils.misc.Time;

/**
 * The following class provides a template on how to "fill in" 
 * some standard MATSim agent methods in order to work with the
 * dynamic AgentLock framework. These methods can also be inserted
 * in another agent using the AbstractLockAgent.
 */
public class LockAgentPlugin {
    /**
     * In general, the state has no meaning for the LockAgent.
     * However, in order for the agent to be able to enter the
     * Netsim, certain states must be set as follows.
     * 
     * Start with an ACTIVITY, so that the agent is passed to the
     * LockEngine.
     */
    private State state = State.ACTIVITY;
    private boolean endLeg = false;
    
    public State getState() {
        return state;
    }
    
    /**
     * This is called by the Netsim when the agent finishes. In
     * order for the agent to be passed to the LockEngine, this 
     * needs to set the new state to ACTIVITY.
     */
    public void endLegAndComputeNextState(double now) {
        state = State.ACTIVITY;
    }
    
    /**
     * This comes from the LockAgent interface and is called just
     * before a leg should be started. This is necessary for the
     * QSim to pass the agent to the Netsim for traffic simulation.
     */
    public void changeStateToLeg() {
        state = State.LEG;
        endLeg = false;
    }
    
    /**
     * This is not used for LockAgents and should almost never be called.
     * The only time it actually is called is in order by QSim to compute
     * the initial simulation start time.
     */
    public double getActivityEndTime() {
        return Time.UNDEFINED_TIME;
    }

    /**
     * This is not used for LockAgents and should never be called.
     */
    public void endActivityAndComputeNextState(double now) {
        throw new UnsupportedOperationException();
    }

    /**
     * This is not used for LockAgents and should never be called.
     */
    public void setStateToAbort(double now) {
        throw new UnsupportedOperationException();
    }
    
    /**
     * This is called when a leg should be ended ...
     */
    public void requestEndLeg() {
        endLeg = true;
    }
    
    public boolean isLockWantingToArriveOnCurrentLink(boolean agentValue) {
        return endLeg || agentValue;
    }
}
