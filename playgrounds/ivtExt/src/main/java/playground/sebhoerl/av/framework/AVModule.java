package playground.sebhoerl.av.framework;

import java.util.Collection;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import playground.sebhoerl.agentfsm.StateMachine;
import playground.sebhoerl.agentlock.AgentLockUtils;
import playground.sebhoerl.agentlock.LockEngine;
import playground.sebhoerl.av.logic.agent.AVAgentFactory;
import playground.sebhoerl.av.logic.agent.AVAgentSource;
import playground.sebhoerl.av.logic.agent.AVFleet;
import playground.sebhoerl.av.logic.passenger.PassengerEngine;
import playground.sebhoerl.av.logic.passenger.PassengerHandler;
import playground.sebhoerl.av.logic.service.RequestFactory;
import playground.sebhoerl.av.logic.service.ServiceFactory;
import playground.sebhoerl.av.logic.service.ServiceManager;
import playground.sebhoerl.av.logic.service.ServiceTracker;
import playground.sebhoerl.av.logic.states.Dropoff;
import playground.sebhoerl.av.logic.states.DropoffDrive;
import playground.sebhoerl.av.logic.states.Idle;
import playground.sebhoerl.av.logic.states.Pickup;
import playground.sebhoerl.av.logic.states.PickupDrive;
import playground.sebhoerl.av.logic.states.Waiting;
import playground.sebhoerl.av.model.dispatcher.Dispatcher;
import playground.sebhoerl.av.model.dispatcher.FIFODispatcher;
import playground.sebhoerl.av.model.dispatcher.HeuristicDispatcher;
import playground.sebhoerl.av.model.distribution.DensityDistributionStrategy;
import playground.sebhoerl.av.model.distribution.DistributionStrategy;
import playground.sebhoerl.av.model.distribution.FirstLegDistributionStrategy;
import playground.sebhoerl.av.model.distribution.RandomDistributionStrategy;
import playground.sebhoerl.av.router.AVCongestedTravelTime;
import playground.sebhoerl.av.router.AVCongestionTracker;
import playground.sebhoerl.av.router.AVFlowDensityRandomizedTravelTime;
import playground.sebhoerl.av.router.AVFlowDensityTracker;
import playground.sebhoerl.av.router.AVFlowDensityTravelTime;
import playground.sebhoerl.av.router.AVFreespeedTravelTime;
import playground.sebhoerl.av.router.AVRandomizedTravelTime;
import playground.sebhoerl.av.router.AVRouterFactory;
import playground.sebhoerl.av.router.AVSpeedTracker;
import playground.sebhoerl.av.router.AVSpeedTravelTime;
import playground.sebhoerl.av.router.AVTravelTime;
import playground.sebhoerl.av.router.AVLinkSpeedMutator;
import playground.sebhoerl.av.scoring.AVScoringFunctionFactory;
import playground.sebhoerl.av.utils.AVTripCounter;

public class AVModule extends AbstractModule {
	public static final String AV_MODE = "av";
	
	public enum DistributionStrategyOption {
		Random, FirstLeg, Density
	};
	
	public enum DispatcherOption {
		FIFO, Heuristic, ParallelHeuristic
	}

	@Override
	public void install() {
		addRoutingModuleBinding(AV_MODE).to(AVRoutingModule.class);
		//addControlerListenerBinding().to(ServiceTracker.class);
		addEventHandlerBinding().to(AVTripCounter.class);
		
		AVConfigGroup config = (AVConfigGroup) this.getConfig().getModule("av");
		
		switch (config.getRouter()) {
		case Freespeed:
		    bind(AVTravelTime.class).toInstance(new AVFreespeedTravelTime());
		    break;
        case Congestion:
            addEventHandlerBinding().to(AVCongestionTracker.class);
            bind(AVTravelTime.class).to(AVCongestedTravelTime.class);
            break;
        case Randomized:
            bind(AVTravelTime.class).to(AVRandomizedTravelTime.class);
            break;
        case FlowDensity:
            addEventHandlerBinding().to(AVFlowDensityTracker.class);
            bind(AVTravelTime.class).to(AVFlowDensityTravelTime.class);
            break;
        case FlowDensityRandomized:
            addEventHandlerBinding().to(AVFlowDensityTracker.class);
            bind(AVTravelTime.class).to(AVFlowDensityRandomizedTravelTime.class);
            break;
        case Speed:
            addEventHandlerBinding().to(AVSpeedTracker.class);
            bind(AVTravelTime.class).to(AVSpeedTravelTime.class);
            break;
		}
	}
	
	@Provides @Singleton
	AVTripCounter provideAVTripCounter(OutputDirectoryHierarchy hierarchy) {
	    return new AVTripCounter(hierarchy);
	}
	
	@Provides @Singleton
	ServiceTracker provideServiceTracker(AVConfigGroup config, OutputDirectoryHierarchy controlerIO) {
	    return new ServiceTracker(config.getDumpServicesInterval(), controlerIO);
	}
	
	@Provides @Singleton
	AVCongestionTracker provideAVCongestionTracker(Network network) {
	    return new AVCongestionTracker(0.0, 30.0 * 3600.0, 30, network);
	}
	
    @Provides @Singleton
    AVFlowDensityTracker provideAVFlowDensityTracker(Network network) {
        return new AVFlowDensityTracker(10.0, network);
    }
    
    @Provides @Singleton
    AVSpeedTracker provideAVSpeedTracker(Network network) {
        return new AVSpeedTracker(network);
    }
	
    @Provides @Singleton
    AVLinkSpeedMutator AVLinkSpeedMutator(AVConfigGroup config) {
        return new AVLinkSpeedMutator(config.getMutatorPath());
    }
	
	@Provides @Singleton
	AVRandomizedTravelTime provideAVRandomizedTravelTime(AVLinkSpeedMutator mutator) {
	    return new AVRandomizedTravelTime(mutator);
	}
	
    @Provides @Singleton
    AVCongestedTravelTime provideAVCongestedTravelTime(AVCongestionTracker tracker) {
        return new AVCongestedTravelTime(tracker);
    }
    
    @Provides @Singleton
    AVFlowDensityTravelTime provideAVFlowDensityTravelTime(AVFlowDensityTracker tracker) {
        return new AVFlowDensityTravelTime(tracker);
    }
    
    @Provides @Singleton
    AVSpeedTravelTime provideAVSpeedTravelTime(AVSpeedTracker tracker) {
        return new AVSpeedTravelTime(tracker);
    }
    
    @Provides @Singleton
    AVFlowDensityRandomizedTravelTime provideAVFlowDensityRandomizedTravelTime(AVFlowDensityTracker tracker, AVLinkSpeedMutator mutator) {
        return new AVFlowDensityRandomizedTravelTime(tracker, mutator);
    }
	
	class InternalModule implements Module {
    	@Provides @Singleton
    	public StateMachine provideStateMachine(AVConfigGroup config, EventsManager events) {
            Idle idleState = new Idle();
            idleState.setEventManager(events);
            
            PickupDrive pickupDriveState = new PickupDrive();
            pickupDriveState.setEventManager(events);
            
            Waiting waitingState = new Waiting();
            waitingState.setEventManager(events);
            
            Pickup pickupState = new Pickup(config.getInteractionConfig().getPickupDuration());
            pickupState.setEventManager(events);
            
            DropoffDrive dropoffDriveState = new DropoffDrive();
            dropoffDriveState.setEventManager(events);
            
            Dropoff dropoffState = new Dropoff(config.getInteractionConfig().getDropoffDuration());
            dropoffState.setEventManager(events);
            
            StateMachine stateMachine = new StateMachine();
            stateMachine.addState("Idle", idleState);
            stateMachine.addState("PickupDrive", pickupDriveState);
            stateMachine.addState("Waiting", waitingState);
            stateMachine.addState("Pickup", pickupState);
            stateMachine.addState("DropoffDrive", dropoffDriveState);
            stateMachine.addState("Dropoff", dropoffState);
            stateMachine.setInitialStateId("Idle");
            
            return stateMachine;
    	}
        
        @Provides @Singleton
        public PassengerEngine providePassengerEngine(EventsManager events) {
            return new PassengerEngine(events);
        }
        
        @Provides @Singleton
        public PassengerHandler providePassengerHandler(PassengerEngine passengerEngine, RequestFactory requestManager, ServiceManager serviceManager) {
            return new PassengerHandler(passengerEngine, requestManager, serviceManager);
        }
        
        @Provides @Singleton
        public ServiceManager provideServiceManager(ServiceFactory serviceFactory, Dispatcher dispatcher) {
            return new ServiceManager(serviceFactory, dispatcher);
        }
        
        @Provides @Singleton
        public ServiceFactory provideServiceFactory() {
            return new ServiceFactory();
        }
        
        @Provides @Singleton
        public RequestFactory provideRequestManager() {
            return new RequestFactory();
        }
        
        @Provides @Singleton
        public AVRouterFactory provideAVRouterFactory(AVTravelTime travelTime, Network network) {
            return new AVRouterFactory(network, travelTime);
        }
        
        @Provides @Singleton
        public Dispatcher provideDispatcher(AVConfigGroup config, AVRouterFactory routerFactory, AVFleet fleet, Scenario scenario, EventsManager events) {
            DispatcherOption strategy = config.getDispatcher();
            
            if (strategy == DispatcherOption.FIFO) {
                return new FIFODispatcher(routerFactory.createRouter(), fleet);
            } else if (strategy == DispatcherOption.Heuristic) {
                HeuristicDispatcher.Config dispatcherConfig = new HeuristicDispatcher.Config();
                
                Collection<? extends ConfigGroup> configs = config.getParameterSets("Dispatcher:Heuristic");
                if (configs.size() > 0) {
                    HeuristicDispatcher.loadConfig(dispatcherConfig, configs.iterator().next());
                }
                
                return new HeuristicDispatcher(dispatcherConfig, fleet, routerFactory, scenario.getNetwork(), events);
            }            
            
            throw new RuntimeException("Unknown Dispatcher: " + strategy);
        }
        
        @Provides @Singleton
        public AVFleet provideFleet(DistributionStrategy strategy) {
            return new AVFleet(strategy);
        }
        
        @Provides @Singleton
        public AVAgentFactory provideAVAgentFactory(EventsManager events, LockEngine lockEngine, StateMachine machine) {
            return new AVAgentFactory(events, lockEngine, machine);
        }
    	
    	@Provides @Singleton
    	public DistributionStrategy provideDistributionStrategy(AVAgentFactory factory, Scenario scenario, AVConfigGroup config) {
    	    DistributionStrategyOption strategy = config.getDistributionStrategy();
    		
    		if (strategy == DistributionStrategyOption.Random) {
    			return new RandomDistributionStrategy(factory, scenario, config.getNumberOfVehicles());
    		} else if (strategy == DistributionStrategyOption.FirstLeg) {
    			return new FirstLegDistributionStrategy(factory, scenario, config.getNumberOfVehicles());
    		} else if (strategy == DistributionStrategyOption.Density) {
    		    return new DensityDistributionStrategy(factory, scenario, config.getNumberOfVehicles());
    		}
    		
    		throw new RuntimeException("Unknown DistributionStrategy: " + strategy);
    	}
        
        @Provides @Singleton
        public LockEngine provideLockEngine(EventsManager events) {
            return new LockEngine(events);
        }

        @Override
        public void configure(Binder binder) {}
    }
	
    @Provides
    public Mobsim provideMobsim(ServiceTracker serviceTracker, Scenario scenario, EventsManager events, Collection<AbstractQSimPlugin> plugins, Injector injector) {
        Injector childInjector = injector.createChildInjector(new InternalModule());
        
        LockEngine lockEngine = childInjector.getInstance(LockEngine.class);
        AVFleet fleet = childInjector.getInstance(AVFleet.class);
        PassengerEngine passengerEngine = childInjector.getInstance(PassengerEngine.class);
        PassengerHandler passengerHandler = childInjector.getInstance(PassengerHandler.class);
        ServiceManager serviceManager = childInjector.getInstance(ServiceManager.class);
        
        QSim qsim = AgentLockUtils.createQSim(scenario, events, plugins, lockEngine);
        
        qsim.addAgentSource(new AVAgentSource(fleet, qsim));
        qsim.addDepartureHandler(passengerHandler);
        qsim.addMobsimEngine(passengerEngine);
        qsim.addMobsimEngine(serviceManager);
        //qsim.addQueueSimulationListeners(serviceTracker);
        
        serviceTracker.setServiceManager(serviceManager);
        
        return qsim;
    }
    
    @Provides @Singleton
    public ScoringFunctionFactory provideAVScoringFunctionFactory(Scenario scenario, AVConfigGroup config) {
        return new AVScoringFunctionFactory(scenario, config);
    }
}
