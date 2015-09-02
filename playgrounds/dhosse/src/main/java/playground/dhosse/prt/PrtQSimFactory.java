package playground.dhosse.prt;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.MatsimVrpContextImpl;
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.path.VrpPathCalculator;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentSource;
import org.matsim.contrib.dvrp.vrpagent.VrpLegs;
import org.matsim.contrib.dvrp.vrpagent.VrpLegs.LegCreator;
import org.matsim.contrib.dynagent.run.DynActivityEngine;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineModule;

import playground.dhosse.prt.launch.NPersonsActionCreator;
import playground.dhosse.prt.launch.PrtParameters.AlgorithmConfig;
import playground.dhosse.prt.passenger.PrtRequestCreator;
import playground.dhosse.prt.request.NPersonsVehicleRequestPathFinder;
import playground.dhosse.prt.scheduler.PrtScheduler;
import playground.michalm.taxi.TaxiActionCreator;
import playground.michalm.taxi.optimizer.TaxiOptimizer;
import playground.michalm.taxi.optimizer.TaxiOptimizerConfiguration;
import playground.michalm.taxi.optimizer.filter.DefaultFilterFactory;
import playground.michalm.taxi.optimizer.filter.FilterFactory;
import playground.michalm.taxi.scheduler.TaxiScheduler;
import playground.michalm.taxi.scheduler.TaxiSchedulerParams;
import playground.michalm.taxi.vehreqpath.VehicleRequestPathFinder;

public class PrtQSimFactory implements MobsimFactory{
	
	private final PrtConfigGroup prtConfig;
	private final MatsimVrpContextImpl context;
	private VrpPathCalculator calculator;
	private AlgorithmConfig algorithmConfig;
	
	public PrtQSimFactory(PrtConfigGroup prtConfig, MatsimVrpContextImpl context, VrpPathCalculator calculator,
			AlgorithmConfig config){
		
		this.prtConfig = prtConfig;
		this.context = context;
		this.calculator = calculator;
		this.algorithmConfig = config;
		
	}

	@Override
	public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
		
		TaxiOptimizerConfiguration taxiConfig = initOptimizerConfiguration(prtConfig, context, calculator, algorithmConfig);
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
	
	private TaxiOptimizerConfiguration initOptimizerConfiguration(PrtConfigGroup prtConfig, MatsimVrpContext context,
			VrpPathCalculator calculator, AlgorithmConfig algorithmConfig){
		
		TaxiSchedulerParams taxiParams = new TaxiSchedulerParams(prtConfig.getDestinationKnown(), false, prtConfig.getPickupDuration(), prtConfig.getDropoffDuration());
		
		if(prtConfig.getVehicleCapacity() > 1){
			
			PrtScheduler scheduler = new PrtScheduler(context, calculator, taxiParams);
			NPersonsVehicleRequestPathFinder vrpFinder = new NPersonsVehicleRequestPathFinder(calculator, scheduler, prtConfig.getVehicleCapacity());
			FilterFactory filterFactory = new DefaultFilterFactory(scheduler, 0, 0);
			
			return new TaxiOptimizerConfiguration(context, calculator, scheduler, vrpFinder, filterFactory,
					algorithmConfig.getGoal(), prtConfig.getPrtOutputDirectory(), null);
			
		}
		
		TaxiScheduler scheduler = new TaxiScheduler(context, calculator, taxiParams);
		VehicleRequestPathFinder vrpFinder = new VehicleRequestPathFinder(calculator, scheduler);
		FilterFactory filterFactory = new DefaultFilterFactory(scheduler, 0, 0);
		
		return new TaxiOptimizerConfiguration(context, calculator, scheduler, vrpFinder, filterFactory,
				algorithmConfig.getGoal(), prtConfig.getPrtOutputDirectory(), null);
		
	}
	
}
