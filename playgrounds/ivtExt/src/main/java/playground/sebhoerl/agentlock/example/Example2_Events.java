package playground.sebhoerl.agentlock.example;

import java.util.Collection;
import java.util.Iterator;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.ActivityEnginePlugin;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import com.google.inject.Provides;

import playground.sebhoerl.agentlock.LockEngine;
import playground.sebhoerl.agentlock.events.Event;
import playground.sebhoerl.agentlock.events.EventListener;
import playground.sebhoerl.agentlock.example.Example1_BasicExample.Router;
import playground.sebhoerl.agentlock.lock.AgentLock;
import playground.sebhoerl.agentlock.lock.LockHandle;

public class Example2_Events {
    /**
     * 
     * ATTENTION: Comments not updated since the last refactoring. Code now works with multithreaded Netsim.
     * 
     * EVENTS
     * ======
     * 
     * Using the lock just gets interesting when events are involved. This means that the
     * agent will remain in the current activity until a certain event is triggered. This 
     * could e.g. be that a taxi is waiting for the car and as soon as the passenger arrives
     * on the pickup link, an event is fired that makes the taxi pick up the passenger.
     * 
     * The AgentLock framework includes a small event library that makes such cases possible. An
     * event is defined as follows:
     * 
     *    Event<EventType>
     *      |       |
     *      |       An arbitrary class, which determines the name of the event and makes it possible
     *      |       to trannsfer arbitrary information within the event.
     *      |
     *      This is the event class itself
     * 
     * The EventType could also be a superordinate state, like a taxi service object, holding information
     * like the pickup location, requested pickup time, dropoff location and so forth.
     * 
     * Furthermore, it is possible to define listener classes by deriving them from EventListener<EventType>
     * and adding them using Event.addListener. Furthermore, inline listeners can be created:
     * 
     *    event.addListener(new EventListener<EventType>() { ... });
     *    
     * Another handy tool is to use a subscription object, which makes it easy to cancel a subscription without
     * having to call addListener and removeListener manually. Also, this way the class that holds the 
     * subscription does not event need to know about the actual event object:
     * 
     *  Subscription subscription = new Subscription(eventListener, event);
     *  subscription.cancel();
     * 
     * There are lots of possibilities to model complex behaviour usign this setup, but here only a
     * short example will be presented. An AgentA will walk around randomly on the network and fire
     * an ArrivalEvent when it crosses link 2. Also, the arrival time will be set as further information.
     * AgentB, on the other side, will just stay at the start link in a "Wait" activity until the event
     * is triggered. Afterwards it will just switch in to a "Done" activity.
     * 
     * For a more "interesting" example one can also see the usage in sebhoerl.agentfsm.example, where
     * the usage in combination with a simple finite state machine is demonstrated.
     */
    
    /**
     * This is the arrival event type, where one can optionally set the arrival time.
     */
    static class ArrivalEvent extends Event {
        public double arrivalTime;
    }
    
    /**
     *  Agent A
     */
    static class AgentA extends LockAgent {
        final private ArrivalEvent arrivalEvent;

        public AgentA(ArrivalEvent arrivalEvent, Router router, LockEngine lockEngine, Id<Person> id, Id<Link> startLinkId) {
            // Pass the arrival event in the constructor
            super(router, lockEngine, id, startLinkId);
            this.arrivalEvent = arrivalEvent;
        }

        @Override
        public LockHandle computeNextState(double now) {
            if (getCurrentLinkId().equals(Id.createLinkId("2"))) {
                // Set additional information to the event
                arrivalEvent.arrivalTime = now;
                
                // First time the agent arrives at 2, fire the event
                arrivalEvent.fire();
            }
            
            // Just go to another random leg
            startLegTo(now, getRandomLinkId());
            return lock.acquireBlocking(State.LEG);
        }
    }
    
    /**
     * Agent B
     */
    static class AgentB extends LockAgent {
        final private ArrivalEvent arrivalEvent;
        
        public AgentB(ArrivalEvent arrivalEvent, Router router, LockEngine lockEngine, Id<Person> id, Id<Link> startLinkId) {
            super(router, lockEngine, id, startLinkId);
            this.arrivalEvent = arrivalEvent;
        }
        
        @Override
        public LockHandle computeNextState(double now) {
            if (!arrivalEvent.hasFired()) {
                // Event has not been fired. Let's wait!
                startActivity(now, "Wait");
                return lock.acquireEvent(State.ACTIVITY, arrivalEvent);
            } else {
                // We're done here
                startActivity(now, "Done");
                return lock.acquireBlocking(State.ACTIVITY);
            }
        }
    }
    
    /**
     * AgentSource, nothing special here
     */
    static class ABAgentSource implements AgentSource {
        private QSim qsim;
        private Router router;
        private LockEngine lockEngine;
        private ArrivalEvent arrivalEvent;
        
        public ABAgentSource(ArrivalEvent arrivalEvent, QSim qsim, Router router, LockEngine lockEngine) {
            this.qsim = qsim;
            this.router = router;
            this.lockEngine = lockEngine;
            this.arrivalEvent = arrivalEvent;
        }
        
        @Override
        public void insertAgentsIntoMobsim() {
            Id<Link> startLinkId = Id.createLinkId("5");
            
            // Create agent A
            AgentA agentA = new AgentA(arrivalEvent, router, lockEngine, Id.createPersonId("AgentA"), startLinkId);
            Vehicle vehicleA = VehicleUtils.getFactory().createVehicle(Id.createVehicleId("AgentA"), VehicleUtils.getDefaultVehicleType());
            
            qsim.createAndParkVehicleOnLink(vehicleA, startLinkId);
            qsim.insertAgentIntoMobsim(agentA);
            
            // Create agent B
            AgentB agentB = new AgentB(arrivalEvent, router, lockEngine, Id.createPersonId("AgentB"), startLinkId);
            Vehicle vehicleB = VehicleUtils.getFactory().createVehicle(Id.createVehicleId("AgentB"), VehicleUtils.getDefaultVehicleType());
            
            qsim.createAndParkVehicleOnLink(vehicleB, startLinkId);
            qsim.insertAgentIntoMobsim(agentB);
        }
    }

    /**
     * This is the main method, similar to example 1.
     * Also it is shown how a listeners can be used.
     */
    public static void main(String[] args) {
        String configPath = "resources/examples/config.xml";
        
        // Load scenario
        Config config = ConfigUtils.loadConfig(configPath);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        
        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {}
            
            @Provides
            Mobsim provideMobsim(Scenario scenario, EventsManager events, Collection<AbstractQSimPlugin> plugins) {
                for (Iterator<AbstractQSimPlugin> iterator = plugins.iterator(); iterator.hasNext();) {
                    if (iterator.next() instanceof ActivityEnginePlugin) {
                        iterator.remove();
                    }
                }
                
                QSim qsim = QSimUtils.createQSim(scenario, events, plugins);
                
                // Simple leg router implementation, see further down
                Router router = new Router(scenario.getNetwork());
                
                /*
                 * Create the Mobsim engine. It needs to know about a time that is beyond
                 * the simulation end time in order to "simulate" initely long events, i.e.
                 * ideling and doing nothing.
                 */
                LockEngine lockEngine = new LockEngine(events);
                qsim.addActivityHandler(lockEngine);
                qsim.addMobsimEngine(lockEngine);
                
                /**
                 * Here the event is created.
                 */
                ArrivalEvent arrivalEvent = new ArrivalEvent();
                
                /**
                 * Here an inline listener is defined
                 */
                arrivalEvent.addListener(new EventListener() {
                    @Override
                    public void notifyEvent(Event event) {
                        ArrivalEvent arrivalEvent = (ArrivalEvent) event;
                        System.out.println(String.format("  THE ARRIVAL EVENT HAS BEEN FIRED at %f!", arrivalEvent.arrivalTime));
                    }
                });
                
                /*
                 * Add the agent source
                 */
                qsim.addAgentSource(new ABAgentSource(arrivalEvent, qsim, router, lockEngine));
                
                return qsim;
            }
            
        });
        
        // Run the simulation!
        controler.run();
    }
    
    /**
     * As in example 1, just implementational necessities beyond that point.
     */
    
    static public Id<Link> getRandomLinkId() {
        return Example1_BasicExample.getRandomLinkId();
    }

    static abstract class LockAgent extends Example1_BasicExample.BaseAgent {
        protected AgentLock lock;
        
        protected LockAgent(Router router, LockEngine lockEngine, Id<Person> id, Id<Link> startLinkId) {
            super(router, id, startLinkId);
            this.lock = new AgentLock(lockEngine, this);
        }

        @Override
        public boolean isWantingToArriveOnCurrentLink() {
            return super.isLockWantingToArriveOnCurrentLink(super.isWantingToArriveOnCurrentLink());
        }
    }
}
