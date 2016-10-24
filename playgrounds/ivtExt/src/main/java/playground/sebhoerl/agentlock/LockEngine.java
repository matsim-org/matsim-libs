package playground.sebhoerl.agentlock;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.InternalInterface;

import playground.sebhoerl.agentlock.agent.LockAgent;
import playground.sebhoerl.agentlock.lock.AgentLock;
import playground.sebhoerl.agentlock.lock.LockHandle;

public class LockEngine extends ActivityEngine {
    private InternalInterface internalInterface;
    
    private final PriorityQueue<LockHandle> endTimeQueue = new PriorityQueue<LockHandle>(new Comparator<LockHandle>() {
        public int compare(LockHandle handle1, LockHandle handle2) {
            return Double.compare(handle1.getEndTime(), handle2.getEndTime());
        }
    });
    
    private Queue<LockAgent> advanceQueue = new LinkedList<LockAgent>();
    
    public LockEngine(EventsManager eventsManager) {
        super(eventsManager, null);
    }

    @Override
    public void doSimStep(double time) {
        // 1. Advance due time-based activities
        
        while (endTimeQueue.peek() != null) {
            if (endTimeQueue.peek().getEndTime() > time) {
                // Top of the queue is in the future. Do nothing now.
                break;
            } 
            
            LockHandle handle = endTimeQueue.poll();
            
            if (handle.isValid()) {
                // If this is not true the entry is deprecated, just drop it then!
                
                if (handle.getState() == MobsimAgent.State.LEG) {
                    // If it is a leg, notify the handle to make the agent end it ...
                    // In case the agent already is just ending right now, this will have no effect
                    
                    handle.getAgent().requestEndLeg();
                } else {
                    internalInterface.unregisterAdditionalAgentOnLink(handle.getAgent().getId(), handle.getAgent().getCurrentLinkId());
                    advanceQueue.add(handle.getAgent());
                }
            }
        }
        
        // 2. Handle all the new states
        
        while (!advanceQueue.isEmpty()) { 
            // TODO: Are those safety measures necessary?
            Queue<LockAgent> safeQueue = advanceQueue;
            advanceQueue = new LinkedList<LockAgent>();
            
            for (LockAgent agent : safeQueue) {
                LockHandle handle = agent.computeNextState(time);
                
                switch (handle.getState()) {
                case LEG:
                    /**
                     * This is a bit tricky here. The agent needs to have the state LEG
                     * for QSim to pass it to the departure handlers. On the other hand,
                     * when it finishes the leg, it needs to have the ACTIVITY state in 
                     * order to return back here to this ActivityHandler.
                     */
                    
                    handle.getAgent().changeStateToLeg();
                    internalInterface.arrangeNextAgentState(agent);
                    break;
                case ABORT:
                    // TODO: Handle this somehow
                    throw new UnsupportedOperationException();
                case ACTIVITY:
                    internalInterface.registerAdditionalAgentOnLink(agent);
                    break;
                }
                
                if (handle.getLockType() == AgentLock.Type.TIME) {
                    // Only if there is a timed lock, we need to keep track of the agent here
                    endTimeQueue.add(handle);
                }
            }
        }
        
        super.doSimStep(time);
    }

    @Override
    public boolean handleActivity(MobsimAgent agent) {
        if (!(agent instanceof LockAgent)) {
            return super.handleActivity(agent);
        }
        
        advanceQueue.add((LockAgent) agent);
        return true;
    }
    
    public void advance(LockAgent agent) {
        advanceQueue.add(agent);
    }
    
    @Override
    public void afterSim() {
        super.afterSim();
        
        endTimeQueue.clear();
        advanceQueue.clear();
    }


    @Override
    public void setInternalInterface(InternalInterface internalInterface) {
        this.internalInterface = internalInterface;
        super.setInternalInterface(internalInterface);
    }
}
