package playground.dhosse.prt;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.*;
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.vrpagent.*;
import org.matsim.contrib.dvrp.vrpagent.VrpLegs.LegCreator;
import org.matsim.contrib.dynagent.run.DynActivityEngine;
import org.matsim.contrib.taxi.TaxiActionCreator;
import org.matsim.contrib.taxi.optimizer.*;
import org.matsim.contrib.taxi.scheduler.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.*;
import org.matsim.core.mobsim.qsim.*;
import org.matsim.core.mobsim.qsim.agents.*;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineModule;
import org.matsim.core.router.util.*;

import playground.dhosse.prt.launch.NPersonsActionCreator;
import playground.dhosse.prt.launch.PrtParameters.AlgorithmConfig;
import playground.dhosse.prt.optimizer.PrtOptimizerConfiguration;
import playground.dhosse.prt.passenger.PrtRequestCreator;
import playground.dhosse.prt.scheduler.PrtScheduler;

public class PrtQSimFactory implements MobsimFactory{
	
	private final PrtConfigGroup prtConfig;
	private final MatsimVrpContextImpl context;
    public final TravelTime travelTime;
    public final TravelDisutility travelDisutility;
	private AlgorithmConfig algorithmConfig;
	

    public PrtQSimFactory(PrtConfigGroup prtConfig, MatsimVrpContextImpl context,
            TravelTime travelTime, TravelDisutility travelDisutility, AlgorithmConfig config)
    {
		
		this.prtConfig = prtConfig;
		this.context = context;
		this.algorithmConfig = config;
		
        this.travelTime = travelTime;
        this.travelDisutility = travelDisutility;
	}

	@Override
	public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
		
		TaxiOptimizerContext taxiConfig = initOptimizerConfiguration(prtConfig, context, travelTime, travelDisutility, algorithmConfig);
		TaxiOptimizer optimizer = algorithmConfig.createTaxiOptimizer(taxiConfig);

		QSimConfigGroup conf = sc.getConfig().qsim();
		if (conf == null) {
            throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
        }
		
		QSim qSim = this.initQSim(sc, eventsManager);
		this.context.setMobsimTimer(qSim.getSimTimer());
		qSim.addQueueSimulationListeners(optimizer);
		
		PassengerEngine passengerEngine = new PassengerEngine(PrtRequestCreator.MODE,
				eventsManager, new PrtRequestCreator(), optimizer, this.context);
		qSim.addMobsimEngine(passengerEngine);
		qSim.addDepartureHandler(passengerEngine);
		
		LegCreator legCreator = this.prtConfig.getUseOnlineVehicleTracker() ? 
				VrpLegs.createLegWithOnlineTrackerCreator(optimizer, qSim.getSimTimer()) :
				    VrpLegs.createLegWithOfflineTrackerCreator(qSim.getSimTimer());
		
		if(this.prtConfig.getVehicleCapacity() > 1){
			
			qSim.addAgentSource(new VrpAgentSource(new NPersonsActionCreator(passengerEngine, legCreator,
					this.prtConfig.getPickupDuration()), this.context, optimizer, qSim));
	        qSim.addAgentSource(new PopulationAgentSource(sc.getPopulation(),
	                new DefaultAgentFactory(qSim), qSim));
	        
		} else{
			
			qSim.addAgentSource(new VrpAgentSource(new TaxiActionCreator(passengerEngine, legCreator,
					this.prtConfig.getPickupDuration()), this.context, optimizer, qSim));
	        qSim.addAgentSource(new PopulationAgentSource(sc.getPopulation(),
	                new DefaultAgentFactory(qSim), qSim));
	        
		}
		
		return qSim;
		
	}

	private QSim initQSim(Scenario scenario, EventsManager events){
		
		QSim qSim = new QSim(scenario,events);
		
		DynActivityEngine dynActivityEngine = new DynActivityEngine(events, qSim.getAgentCounter());
		qSim.addMobsimEngine(dynActivityEngine);
		qSim.addActivityHandler(dynActivityEngine);
		
		QNetsimEngineModule.configure(qSim);
		
		qSim.addMobsimEngine(new TeleportationEngine(scenario, events));
		
		if(scenario.getConfig().network().isTimeVariantNetwork()){
			qSim.addMobsimEngine(new NetworkChangeEventsEngine());
		}
		
		return qSim;
		
	}
	
	private TaxiOptimizerContext initOptimizerConfiguration(PrtConfigGroup prtConfig, MatsimVrpContext context,
			TravelTime travelTime, TravelDisutility travelDisutility, AlgorithmConfig algorithmConfig){
		
		TaxiSchedulerParams taxiParams = new TaxiSchedulerParams(prtConfig.getDestinationKnown(), false, prtConfig.getPickupDuration(), prtConfig.getDropoffDuration(), 1);
		
		if(prtConfig.getVehicleCapacity() > 1){
			
			PrtScheduler scheduler = new PrtScheduler(context, taxiParams, travelTime, travelDisutility);
			
			return new PrtOptimizerConfiguration(context, travelTime, travelDisutility, null, scheduler, prtConfig);
			
		}
		
		TaxiScheduler scheduler = new TaxiScheduler(context, taxiParams, travelTime, travelDisutility);
		
		return new TaxiOptimizerContext(context, travelTime, travelDisutility, null, scheduler);
		
	}
	
}
