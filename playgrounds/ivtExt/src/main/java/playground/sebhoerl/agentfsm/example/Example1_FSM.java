package playground.sebhoerl.agentfsm.example;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.logging.Logger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.QSim;
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
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import com.google.inject.Provides;

import playground.sebhoerl.agentfsm.StateMachine;
import playground.sebhoerl.agentfsm.agent.FSMAgent;
import playground.sebhoerl.agentfsm.agent.FSMAgentImpl;
import playground.sebhoerl.agentfsm.instruction.AdvanceInstruction;
import playground.sebhoerl.agentfsm.instruction.Instruction;
import playground.sebhoerl.agentfsm.instruction.WaitBlockingInstruction;
import playground.sebhoerl.agentfsm.instruction.WaitUntilInstruction;
import playground.sebhoerl.agentfsm.state.AbstractState;
import playground.sebhoerl.agentfsm.state.ActivityState;
import playground.sebhoerl.agentfsm.state.LegState;
import playground.sebhoerl.agentlock.AgentLockUtils;
import playground.sebhoerl.agentlock.LockEngine;
import playground.sebhoerl.agentlock.events.Event;
import playground.sebhoerl.agentlock.events.EventListener;
import playground.sebhoerl.av.router.GenericLeg;

public class Example1_FSM {
    /**
     * 
     * ATTENTION: Comments not updated since the last refactoring. Code now works with multithreaded Netsim.
     * 
     * Agent FINITE STATE MACHINE
     * ==========================
     * 
     * This library is a small kind-of finite state machine implementation,
     * where one can control dynamic agents in terms of functional states and
     * transitions between them. It is based on the AgentLock, which defines
     * until when agents stay in their respective states.
     * 
     * A state is defined by an "enter" method and a "leave" method. Both can
     * execute arbitrary code. A state can either be an ActivityState or an
     * LegState, which also has to provide a leg using the createLeg method.
     * 
     * The following classes will show an example where the job cycle for an
     * ambulance is modelled.
     */
    
    /**
     * This is the internal agent state that is going to be used in the state
     * machine.
     */
    static class Emergency {
        final public Id<Link> linkId; // The link of the emergency
        final public double duration; // How long it will take to handle it
        
        final public Event jobDoneEvent = new Event(); // Will be used to notify the dispatcher
        
        final public double callTime; // The time when the emergency call was made
        public double arrivalTime; // Time when the ambulance arrived
        
        public Emergency(double duration, Id<Link> linkId, double callTime) {
            this.linkId = linkId;
            this.duration = duration;
            this.callTime = callTime;
        }
    }
    
    /**
     * This is the base ambulance state, all ambulance states are
     * derived from it. Its task is to provide the field "emergency"
     * which holds all the information for a job and provides easy
     * access to it.
     * 
     * Before each call to enter or leave, the State object has to be
     * adapted to the current "internal" state of the agent.
     * 
     * The AbstractState class, on the other hand is preparing the agent 
     * and lock handles and processes the requested state transitions 
     * by providing the AbstractState.advance method.
     * 
     * Furthermore, it prepares some general purpose fields, which are:
     * 
     *    - FSMAgent agent
     *         Contains the agent object for which the current state should
     *         be processed.
     *         
     *    - AgentLock lock
     *         The lock of the agent, to control the execution of actions.
     *         
     *    - double now
     *         The current time at which the action occurs (enter/leave)
     * 
     * The AbstractState provides a convenient structure for working with states.
     * From a design perspective it is not the ideal way to expose the agent and
     * lock objects through protected member variables, but in order to make the
     * State classes as slim as possible, it is used here. However, if one does
     * not use this convenience class, the structure is defined in a clear and
     * logic way by the FSMState interface, for which other implementations can
     * be done easily.
     */
    abstract static class AmbulanceState extends AbstractState {
        private AmbulanceRouter router;
        protected Emergency emergency;
        
        /**
         * Expose the emergency object from the agent to the actual agent 
         * states.
         */
        @Override
        public Instruction enter(double now, FSMAgent agent) {
            emergency = ((AmbulanceAgent)agent).getEmergency();
            return super.enter(now, agent);
        }

        @Override
        public AdvanceInstruction leave(double now, FSMAgent agent) {
            emergency = ((AmbulanceAgent)agent).getEmergency();
            return super.leave(now, agent);
        }
        
        /**
         * State objects are "unstateful". It means that they don't hold 
         * information about the state of any agent, but merely define the
         * state as an abstract idea, providing the instructions on how
         * to handle an internal state, which e.g. is described by the
         * emergency object in this case. As one can see, the enter and leave
         * methods update it for every call by getting information from the
         * agent.
         * 
         * In order to avoid cluttering of the actual state classes, other
         * services are not injected using the constructor, but through setter
         * methods, as done here in order to provide an easy routing facility.
         */
        public void setRouter(AmbulanceRouter router) {
            this.router = router;
        }
        
        protected Leg createLegTo(Id<Link> destinationLinkId) {
            Id<Link> originLinkId = agent.getCurrentLinkId();
            NetworkRoute route = router.getRoute(originLinkId, destinationLinkId);
            
            Leg leg = new GenericLeg(TransportMode.car);
            leg.setDepartureTime(0.0);
            leg.setTravelTime(route.getTravelTime());
            leg.setRoute(route);
            return leg;
        }
    }
    
    /**
     * What follows are the definitions of the actual states.
     * 
     * Idle state. It makes the agent block until it is woken up.
     */
    static class IdleState extends AmbulanceState implements ActivityState {
        @Override
        public Instruction enter() {
            logger.info(String.format("@%f %s goes on IDLE", now, agent.getId()));
            return new WaitBlockingInstruction();
        }
        
        @Override
        public AdvanceInstruction leave() {
            // This call makes the state machine proceed to another state within the
            // same simulation step. If "advance" is called in the "enter" method, 
            // it means that the current state is aborted directly and the next state 
            // is evaluated right after it. So essentially it would be skipped.
            return new AdvanceInstruction("DriveToEmergency");
        }
    }
    
    /**
     * Drive to emergency.
     * 
     * This is a LegState, which provides the leg that should be performed through
     * the createLeg method.
     */
    static class DriveToEmergencyState extends AmbulanceState implements LegState {
        @Override
        public Instruction enter() {
            // Drive to the emergency
            logger.info(String.format("@%f %s drives to %s", now, agent.getId(), emergency.linkId.toString()));
            return new WaitBlockingInstruction();
        }
        
        @Override
        public AdvanceInstruction leave() {
            // Ambulance has arrived
            emergency.arrivalTime = now;
            return new AdvanceInstruction("HandleEmergency");
        }
        
        @Override
        public Leg createLeg() {
            return createLegTo(emergency.linkId);
        }
    }
    
    /**
     * Perform activity at the emergency.
     */
    static class HandleEmergencyState extends AmbulanceState implements ActivityState {
        @Override
        public Instruction enter() {
            // It takes a predefined duration to handle the emergency
            logger.info(String.format("@%f %s handles an emergency", now, agent.getId()));
            return new WaitUntilInstruction(now + emergency.duration);
        }
        
        @Override
        public AdvanceInstruction leave() {
            return new AdvanceInstruction("DriveToStation");
        }
    }   
    
    /**
     * Drive back to the station.
     * 
     * Notice the constructor injection here, which is specific to
     * this abstract state. This is useful if only particular states
     * need access to service objects from the MATSim simulation (or to
     * configuration options, as in this case).
     */
    static class DriveToStationState extends AmbulanceState implements LegState {
        private Id<Link> stationLinkId;
        
        public DriveToStationState(Id<Link> stationLinkId) {
            this.stationLinkId = stationLinkId;
        }
        
        @Override
        public Instruction enter() {
            // Drive to the emergency
            logger.info(String.format("@%f %s drives home", now, agent.getId()));
            return new WaitBlockingInstruction();
        }
        
        @Override
        public AdvanceInstruction leave() {
            // Notify the dispatcher that the ambulance is done!
            emergency.jobDoneEvent.fire();
            return new AdvanceInstruction("Idle");
        }
        
        @Override        
        public Leg createLeg() {
            return createLegTo(stationLinkId);
        }
    }
    
    /**
     * The definition of the agent can be really simple:
     */
    static class AmbulanceAgent extends FSMAgentImpl {
        private Emergency emergency;
        
        public AmbulanceAgent(EventsManager events, LockEngine lockEngine, StateMachine machine, Id<Person> id, Id<Link> startLinkId) {
            super(events, lockEngine, machine, id, startLinkId);
        }
        
        /**
         * This will be called by the dispatcher.
         */
        public void handleEmergency(Emergency emergency) {
            if (!controller.getCurrentStateId().equals("Idle")) {
                throw new IllegalStateException();
            }
            
            this.emergency = emergency;
            
            // Release the agent lock to leave the Idle state
            controller.release();
        }
        
        public Emergency getEmergency() {
            return emergency;
        }
        
        @Override
        public Id<Vehicle> getPlannedVehicleId() {
            return Id.createVehicleId(getId().toString());
        }
    }
    
    /**
     * Agent source to insert the ambulance into the simulation
     */
    static class AmbulanceSource implements AgentSource {
        private QSim qsim;
        private EventsManager events;
        private LockEngine lockEngine;
        private Id<Link> stationLinkId;
        private StateMachine machine;
        private Dispatcher dispatcher;
        
        public AmbulanceSource(QSim qsim, EventsManager events, LockEngine lockEngine, StateMachine machine, Dispatcher dispatcher, Id<Link> stationLinkId) {
            this.qsim = qsim;
            this.lockEngine = lockEngine;
            this.events = events;
            this.stationLinkId = stationLinkId;
            this.machine = machine;
            this.dispatcher = dispatcher;
        }

        /**
         * In order to use the FSM, the agent needs an arbitrary AgentLock and a FSMAgentController. However, the 
         * controller and the FSMAgentImpl are merely convenience classes, they can be reimplemented altogether.
         */
        @Override
        public void insertAgentsIntoMobsim() {
            for (int i = 1; i <= NUMBER_OF_AMBULANCES; i++) {
                AmbulanceAgent agent = new AmbulanceAgent(events, lockEngine, machine, Id.createPersonId("Ambulance" + String.valueOf(i)), stationLinkId);
                Vehicle vehicle = VehicleUtils.getFactory().createVehicle(Id.createVehicleId("Ambulance" + String.valueOf(i)), VehicleUtils.getDefaultVehicleType());
                
                qsim.createAndParkVehicleOnLink(vehicle, stationLinkId);
                qsim.insertAgentIntoMobsim(agent);
                
                dispatcher.addAgent(agent);
            }
        }
    }
    
    /**
     * Dispatcher, which will randomly generate emergencies from time to time.
     */
    public static class Dispatcher implements MobsimAfterSimStepListener, MobsimBeforeCleanupListener {
        private Queue<AmbulanceAgent> availableAgents = new LinkedList<AmbulanceAgent>();
        private Queue<Emergency> unhandledEmergencies = new LinkedList<Emergency>();
        
        private double serviceTimeSum = 0.0;
        private double serviceCount = 0.0;
        
        // Add agents to the dispatcher
        void addAgent(AmbulanceAgent agent) {
            availableAgents.offer(agent);
        }
        
        @SuppressWarnings("rawtypes")
        @Override
        public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
            // Randomly create emergencies from time to time
            if (random.nextDouble() <= EMERGENCY_PROBABILITY_PER_SECOND) {
                Emergency emergency = new Emergency(
                        random.nextDouble() * 2 * 3600.0, 
                        getRandomLinkId(), 
                        e.getSimulationTime());
                
                unhandledEmergencies.offer(emergency);
            }
            
            // If there are unhandled requests and ambulances available, assign them!
            while (unhandledEmergencies.size() > 0 && availableAgents.size() > 0) {
                final AmbulanceAgent agent = availableAgents.poll();
                final Emergency emergency = unhandledEmergencies.poll();
                
                // EventListener: When the ambulance is done, add it back to the available queue.
                emergency.jobDoneEvent.addListener(new EventListener() {
                    @Override
                    public void notifyEvent(Event event) {
                        availableAgents.offer(agent);
                        
                        // Raise some statistics
                        serviceTimeSum += emergency.arrivalTime - emergency.callTime;
                        serviceCount += 1.0;
                    }
                });
                
                // Let the agent handle the emergency!
                agent.handleEmergency(emergency);
            }
        }

        @SuppressWarnings("rawtypes")
        @Override
        public void notifyMobsimBeforeCleanup(MobsimBeforeCleanupEvent e) {
            logger.info(String.format("SUMMARY: The average service time was %f!", serviceTimeSum / serviceCount));
        }
    }
    
    final static double EMERGENCY_PROBABILITY_PER_SECOND = 1.0 / 3600.0;
    final static int NUMBER_OF_AMBULANCES = 5;
    
    /**
     * Here the simulation is started and the FSM is constructed.
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
            LockEngine provideLockEngine(EventsManager events) {
                return new LockEngine(events);
            }
            
            @Provides
            Mobsim provideMobsim(LockEngine lockEngine, Scenario scenario, EventsManager events, Collection<AbstractQSimPlugin> plugins) {
                QSim qsim = AgentLockUtils.createQSim(scenario, events, plugins, lockEngine);
                
                // Simple leg router implementation, see further down
                AmbulanceRouter router = new AmbulanceRouter(scenario.getNetwork());
                
                // Define the link id of the station. Something like this could come from the config.
                Id<Link> stationLinkId = Id.createLinkId("5");
                
                /**
                 * Here the state machine is created
                 */
                StateMachine machine = new StateMachine();
                
                IdleState idleState = new IdleState();
                DriveToEmergencyState driveToEmergencyState = new DriveToEmergencyState();
                HandleEmergencyState handleEmergencyState = new HandleEmergencyState();
                DriveToStationState driveToStationState = new DriveToStationState(stationLinkId); // Config injection!
                
                idleState.setRouter(router);
                driveToEmergencyState.setRouter(router);
                handleEmergencyState.setRouter(router);
                driveToStationState.setRouter(router);
                
                machine.addState("Idle", idleState);
                machine.addState("DriveToEmergency", driveToEmergencyState);
                machine.addState("HandleEmergency", handleEmergencyState);
                machine.addState("DriveToStation", driveToStationState);
                
                machine.setInitialStateId("Idle"); // Don't forget this!
                
                // Register the dispatcher
                Dispatcher dispatcher = new Dispatcher();
                qsim.addQueueSimulationListeners(dispatcher);
                
                // Add the agent source
                qsim.addAgentSource(new AmbulanceSource(qsim, events, lockEngine, machine, dispatcher, stationLinkId));
                
                return qsim;
            }
            
        });
        
        // Run the simulation!
        controler.run();    
    }
    
    /** 
     * Implementation details beyond this line. Not much interesting. 
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
    
    static class AmbulanceRouter {
        final private Network network;
        final private LeastCostPathCalculator pathCalculator;
        
        public AmbulanceRouter(Network network) {
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
