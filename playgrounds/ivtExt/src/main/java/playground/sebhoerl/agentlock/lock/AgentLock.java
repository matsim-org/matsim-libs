package playground.sebhoerl.agentlock.lock;

import org.matsim.core.mobsim.framework.MobsimAgent;

import playground.sebhoerl.agentlock.LockEngine;
import playground.sebhoerl.agentlock.agent.LockAgent;
import playground.sebhoerl.agentlock.events.Event;
import playground.sebhoerl.agentlock.events.EventListener;
import playground.sebhoerl.agentlock.events.Subscription;

public class AgentLock {
    public enum Type {
        RELEASED, BLOCKING, TIME, EVENT
    }
    
    final private LockAgent agent;
    final private LockEngine engine;
    
    private long index = 0;
    private Type type = Type.RELEASED;
    private MobsimAgent.State state;
    
    private Subscription acquiredSubscription;
    
    public AgentLock(LockEngine engine, LockAgent agent) {
        this.agent = agent;
        this.engine = engine;
    }
    
    protected void resetSubscription() {
        if (acquiredSubscription != null) {
            acquiredSubscription.cancel();
            acquiredSubscription = null;
        }
    }
    
    protected void prepare(MobsimAgent.State state, Type type) {
        resetSubscription();
        this.state = state;
        this.type = type;
        index++;
    }
    
    public LockHandle acquireBlocking(MobsimAgent.State state) {
        prepare(state, Type.BLOCKING);
        return new LockHandleImpl(agent, this, state, 0.0);
    }
    
    public LockHandle acquireUntil(MobsimAgent.State state, double time) {
        prepare(state, Type.TIME);
        return new LockHandleImpl(agent, this, state, time);
    }
    
    public LockHandle acquireEvent(MobsimAgent.State state, Event event) {
        prepare(state, Type.EVENT);
        
        acquiredSubscription = Subscription.create(event, new EventListener() {
            @Override
            public void notifyEvent(Event event) {
                release();
            }
        });
        
        return new LockHandleImpl(agent, this, state, 0.0);
    }
    
    public void release() {
        if (type == Type.RELEASED) return;
        prepare(state, Type.RELEASED);

        if (state == MobsimAgent.State.LEG) {
            agent.requestEndLeg();
        } else {
            engine.advance(agent);
        }
    }
    
    public Type getType() {
        return type;
    }
    
    public long getIndex() {
        return index;
    }
}
