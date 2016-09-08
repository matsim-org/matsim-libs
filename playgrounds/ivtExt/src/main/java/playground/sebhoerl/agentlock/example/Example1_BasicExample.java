package playground.sebhoerl.agentlock.example;

import java.util.Collection;
import java.util.Iterator;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.ActivityEnginePlugin;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.facilities.Facility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import com.google.inject.Provides;

import playground.sebhoerl.agentlock.LockEngine;
import playground.sebhoerl.agentlock.agent.AbstractLockAgent;
import playground.sebhoerl.agentlock.lock.AgentLock;
import playground.sebhoerl.agentlock.lock.LockHandle;

public class Example1_BasicExample {
    /**
     * 
     * ATTENTION: Comments not updated since the last refactoring. Code now works with multithreaded Netsim.
     * 
     * MOTIVATION
     * ==========
     * 
     * By default MATSim allows two kinds of states: What is next for an agent is determined using
     * endLegAndComputeNextState and endActivityAndComputeNextState. The the engine checks using
     * MobsimAgent.getState if the next step is an activity or a leg. From then, the agent is not
     * touched anymore until a certain criterion is reached: Either the agent notifies using
     * isWantingToArriveOnCurrentLink that he wants to end the leg or the queue simulation will "wake up"
     * the agent after a time that is specified through getActivityEndTime. 
     * 
     * So while a leg could be ended dynamically at least for every link the agent is moving to, there
     * is no internal way for an action to end earlier by default (and this is kind of what makes the
     * queue simulation fast). However, in some cases one might like to implement dynamic agents, which act 
     * on certain conditions, e.g. waiting for another agent to arrive at a certain link before anything else 
     * is done. In that case one would want the agent to idle and not do anything and at some point wake it up 
     * conditionally.
     * 
     * The queue simulation has a function rescheduleAgentActivity, which can be used to remove an agent from
     * the activity queue and reinsert it again with a new scheduled end time. This is a useful tool for this 
     * purpose, but e.g. in order to make an agent abort the current activity and let the engine proceed to
     * the next one requires some steps, which have to be done INTERNALLY in the agent and EXTERNALLY in the 
     * QSim:
     * 
     *     - Make sure that MobsimAgent.getActivityEndTime returns "now" or "0.0"
     *     - Call QSim.rescheduleAgentActivity
     * 
     * If one creates an agent with a lot of different dynamic tasks, it can get tedious to issue and synchronize
     * all the relevant calls. Therefore it would be nice to have a small framework, which is handling those
     * actions and allowing agents to control their activities event-based instead of time-based. A general
     * purpose solution for this is the AgentLock. What has to be taken into account is, that the agent should
     * actually not have access to the whole QSim and only be able to use a small functional interface.
     */
    
    /**
     * BASIC USAGE
     * ===========
     * 
     * In order to use the AgentLock, it somehow needs to be passed to an agent (best at initialization) and then 
     * this object is hookng into the isWantingToArriveOnCurrentLink and getActivityEndTime methods of the 
     * agent. When setting up the next state (activity or leg) one needs to define how the agent will behave subsequently.
     * This is done by calling one of three special methods before the end*AndCalculateNextState has finished.
     * 
     * These are:
     * 
     * - AgentLock.acquireBlocking()
     * 
     *     . If the agent goes on a leg, the next state will be calculated when it has finished it.
     *     . If the agent goes on an activity, it will stay in this activity indefinitely!
     *     
     *     This is also the default behaviour if no other lock mode is selected.
     *     
     *  - AgentLock.acquireUntil(double time)
     *  
     *     . If the agent goes on a leg, this will throw an exception so far (but the behaviour could
     *       be enriched later on in development, depends how useful it would be)
     *     . If the agent goes on an activity, it will stay in this activity until "time" is reached.
     *       This resembles the standard MATSim behaviour for activies.
     *       
     *  - AgentLock.acquireEvent(Event<EventType> event)
     *  
     *     . If the agent goes on a leg, it will be ended as soon as it is finished, or as soon as
     *       the event happens.
     *     . If the agent goes on an activity, it will be ended as soon as the event happens.
     *     
     *       More on the events will be explained later.
     *       
     * Moreover, when an agent is in a waiting for blocking condition, it can be woken up manually if 
     * one has access to the AgentLock of the agent. Then one has the option to call AgentLock.release
     * which will make sure that the agent stops what it is doing right now (leg or activity).
     */
    
    /**
     * EXAMPLE 1: Walking around
     * 
     * This example simulates agents, who are just driving around on a network and from time to
     * time stop to refuel their car. The first class contains all the relevant code for the AgentLock 
     * and the specific agent logic. All standard stuff like traveling on the leg is moved to BaseAgent, 
     * further down the file.
     * 
     * Afterwards it is shown how the simulation needs to be setup for this to work.
     */
    
    static class RandomAgent extends BaseAgent {
        private AgentLock lock;
        private double fuel = 10.0;
        
        public RandomAgent(Router router, LockEngine lockEngine, Id<Person> id, Id<Link> startLinkId) {
            super(router, id, startLinkId);
            lock = new AgentLock(lockEngine, this);
        }

        @Override
        public LockHandle computeNextState(double now) {
            /*
             * This is the agent logic:
             * 
             *    - If the fuel level is over 0.0, the agent will
             *      perform a leg to a random node, i.e. it will
             *      just drive around in the network. But it will
             *      decrease the fuel level for every leg.
             *    - If the fuel level is under 0.0, the aget goes
             *      a refueling activity for a certain amount of
             *      time and finishes it with a full fuel level of 10.
             */
            
            if (fuel < 0.0) { // No more fuel
                fuel = 10.0;
                
                // Set the state to ACTIVITY
                startActivity(now, "Refuel!");
                
                // Lock the state for 300 seconds
                return lock.acquireUntil(State.ACTIVITY, now + 300.0);
            } else {
                // Fuel is decreased
                fuel -= random.nextDouble();
                
                // Just go to a random link
                startLegTo(now, getRandomLinkId());
                
                // Lock the state until finished
                return lock.acquireBlocking(State.LEG);
            }
        }

        @Override
        public boolean isWantingToArriveOnCurrentLink() {
            /**
             * Here the following is going on: From the AbstractLockAgent there is a method
             * 
             *    isLockWantingToArriveOnCurrentLink(boolean agentValue)
             * 
             * This will return true if either the agent wants to quit (e.g. because the leg
             * is finished) or because the lock has been released (manually, by time, event, ...)
             * 
             * The inner method
             * 
             *    isWantingToArriveOnCurrentLink()
             * 
             * comes just from the BaseAgent further down the file, which checks if a leg should
             * be finished because the agent arrived at the desired position.
             * 
             * So in general: 
             */
            return super.isLockWantingToArriveOnCurrentLink(super.isWantingToArriveOnCurrentLink());
        }
    }
    
    /**
     * Agent source to insert them into the simulation
     */
    static class RandomAgentSource implements AgentSource {
        private QSim qsim;
        private Router router;
        private LockEngine lockEngine;
        
        public RandomAgentSource(QSim qsim, Router router, LockEngine lockEngine) {
            this.qsim = qsim;
            this.router = router;
            this.lockEngine = lockEngine;
        }
        
        void insertAgent(Id<Person> id) {
        	Id<Link> linkId = getRandomLinkId();
        	
            RandomAgent agent = new RandomAgent(router, lockEngine, id, linkId);
            Vehicle vehicle = VehicleUtils.getFactory().createVehicle(Id.createVehicleId(id), VehicleUtils.getDefaultVehicleType());
            
            qsim.createAndParkVehicleOnLink(vehicle, linkId);
            qsim.insertAgentIntoMobsim(agent);
        }
        
        @Override
        public void insertAgentsIntoMobsim() {
            for (int i = 1; i < 11; i++) {
                Id<Person> id = Id.createPersonId(String.format("agent%d", i));
                insertAgent(id);
            }
        }
    }
    
    /**
     * This is the main function, which builds up the whole simulation. What needs to be done here is to
     * register the ReschedulingAgentLockEngine as a Mobsim. This is only one possible implementation, it
     * is possible to decouple it completely from the rescheduling, e.g. similar to the DynAgent and DVRP
     * approach.
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
                
                /*
                 * The factory creating the agent-specific locks
                 */
                //LockFactory lockFactory = new ReschedulingAgentLockFactory(lockEngine);
                
                /*
                 * Add the agent source
                 */
                qsim.addAgentSource(new RandomAgentSource(qsim, router, lockEngine));
                
                return qsim;
            }
            
        });
        
        // Run the simulation!
        controler.run();
    }
    
    
    /**
     * The following part of the file is just necessary for the functioning of the example, it
     * is just standard implementation stuff.
     */
    
    static Random random = new Random();
    static Logger logger = Logger.getAnonymousLogger();
    
    static Id<Link> getRandomLinkId() {
    	String ids[] = {
    			"1", "2", "3", "4", "5", "6", "7", "8", "x1", "x2",
    			"1r", "2r", "3r", "4r", "5r", "6r", "7r", "8r", "x1r", "x2r"
    			};
    	
        return Id.createLinkId(ids[random.nextInt(20)]);
    }
    
    /**
     * This is a basic agent that has two internal methods:
     * 
     *    - startLeg let's the agent go on a leg
     *    - startActivity let's the agent on an activity
     *    
     * All the necessary functionality is implemented to travle through
     * legs by car. What needs to be implemented is
     * 
     *    - computeNextState in order to set the desired state
     */
    static abstract class BaseAgent extends AbstractLockAgent implements MobsimDriverAgent {
        final private Id<Person> id;
        
        private Id<Link> currentLinkId;
        private NetworkRoute route;
        private int routeIndex = 0;
        private MobsimVehicle vehicle;
        private Router router;
        
        protected BaseAgent(Router router, Id<Person> id, Id<Link> startLinkId) {
            this.id = id;
            this.currentLinkId = startLinkId;
            this.router = router;
        }
        
        protected void startLegTo(double now, Id<Link> linkId) {
            route = router.getRoute(currentLinkId, linkId);
            routeIndex = 0;
            logger.log(Level.INFO, String.format("@%f - %s - Start leg from %s to %s", now, id, currentLinkId, linkId));
        }
        
        protected void startActivity(double now, String description) {
            logger.log(Level.INFO, String.format("@%f - %s - Start activity %s", now, id, description));
        }

        @Override
        public Double getExpectedTravelTime() {
            // No teleportation
            throw new UnsupportedOperationException();
        }

        @Override
        public Double getExpectedTravelDistance() {
         // No teleportation
            throw new UnsupportedOperationException();
        }

        @Override
        public void notifyArrivalOnLinkByNonNetworkMode(Id<Link> linkId) {
         // No teleportation
            throw new UnsupportedOperationException();
        }

        @Override
        public Id<Link> getCurrentLinkId() {
            return currentLinkId;
        }

        @Override
        public Id<Link> getDestinationLinkId() {
            return route.getEndLinkId();
        }

        @Override
        public String getMode() {
            return TransportMode.car;
        }

        @Override
        public Id<Person> getId() {
            return id;
        }

        @Override
        public Id<Link> chooseNextLinkId() {
            if (routeIndex >= route.getLinkIds().size()) {
                return route.getEndLinkId();
            }
            
            return route.getLinkIds().get(routeIndex);
        }

        @Override
        public void notifyMoveOverNode(Id<Link> newLinkId) {
            currentLinkId = newLinkId;
            routeIndex++;
        }

        @Override
        public boolean isWantingToArriveOnCurrentLink() {
            return currentLinkId.equals(route.getEndLinkId());
        }

        @Override
        public void setVehicle(MobsimVehicle veh) {
            vehicle = veh;
        }

        @Override
        public MobsimVehicle getVehicle() {
            return vehicle;
        }

        @Override
        public Id<Vehicle> getPlannedVehicleId() {
            return Id.create(id.toString(), Vehicle.class);
        }
        
        @Override
        public Facility<? extends Facility<?>> getCurrentFacility() {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public Facility<? extends Facility<?>> getDestinationFacility() {
            throw new UnsupportedOperationException();
        }
    }
    
    
    /**
     * Simple router implementation, not very interesting...
     */
    static class Router {
        final private Network network;
        final private LeastCostPathCalculator pathCalculator;
        
        public Router(Network network) {
            this.network = network;
            
            TravelTime travelTime = new FreeSpeedTravelTime();
            pathCalculator = new Dijkstra(network, new OnlyTimeDependentTravelDisutility(travelTime), travelTime);
        }
        
        public NetworkRoute getRoute(Id<Link> fromLinkId, Id<Link> toLinkId) {
            // see {@org.matsim.core.router.NetworkRoutingModule}
            
            Node startNode = network.getLinks().get(fromLinkId).getToNode();
            Node endNode = network.getLinks().get(toLinkId).getFromNode();
            Path path = pathCalculator.calcLeastCostPath(startNode, endNode, 0.0, null, null);
            
            NetworkRoute route = new LinkNetworkRouteImpl(fromLinkId, NetworkUtils.getLinkIds(path.links), toLinkId);
            route.setTravelTime(path.travelTime);
            route.setTravelCost(path.travelCost);
            route.setDistance(RouteUtils.calcDistance(route, 0.0, 0.0, network));
            
            return route;
        }
    }
    
}










