package playground.dhosse.prt;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.*;
import org.matsim.contrib.dvrp.router.*;
import org.matsim.contrib.dvrp.run.*;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils.TravelDisutilitySource;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils.TravelTimeSource;
import org.matsim.contrib.dvrp.util.time.TimeDiscretizer;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.*;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

import playground.dhosse.prt.launch.PrtParameters.AlgorithmConfig;
import playground.dhosse.prt.passenger.PrtRequestCreator;
import playground.dhosse.prt.request.NPersonsVehicleRequestPathFinder;
import playground.dhosse.prt.router.PrtTripRouterFactoryImpl;
import playground.dhosse.prt.scheduler.PrtScheduler;
import playground.michalm.taxi.data.TaxiData;
import playground.michalm.taxi.optimizer.*;
import playground.michalm.taxi.optimizer.filter.*;
import playground.michalm.taxi.run.TaxiLauncherUtils;
import playground.michalm.taxi.scheduler.*;
import playground.michalm.taxi.vehreqpath.VehicleRequestPathFinder;

public class PrtModule {
	
	public void configureControler(Controler controler){
		
		PrtConfigGroup prtConfig = ConfigUtils.addOrGetModule(controler.getConfig(), PrtConfigGroup.GROUP_NAME, PrtConfigGroup.class);
		
		Scenario scenario = VrpLauncherUtils.initScenario(controler.getConfig().network().getInputFile(),
				controler.getConfig().plans().getInputFile(), prtConfig.getEventsFile());
		
		AlgorithmConfig algorithmConfig = AlgorithmConfig.valueOf(prtConfig.getAlgorithmConfig());
		
		TravelTimeSource ttimeSource = algorithmConfig.getTravelTimeSource();
		TravelDisutilitySource tdisSource = algorithmConfig.getTravelDisutilitySource();
		
		TravelTime ttime = new FreeSpeedTravelTime();
		TravelDisutility tdis = VrpLauncherUtils.initTravelDisutility(tdisSource, ttime);
		
		LeastCostPathCalculator router = new Dijkstra(scenario.getNetwork(), tdis, ttime);
		LeastCostPathCalculatorWithCache routerWithCache = new LeastCostPathCalculatorWithCache(router, new TimeDiscretizer(30*4, 15, false));
		
		VrpPathCalculator calculator = new VrpPathCalculatorImpl(routerWithCache, ttime, tdis);
		
		MatsimVrpContextImpl context = new MatsimVrpContextImpl();
		context.setScenario(scenario);
		
		TaxiData taxiData = TaxiLauncherUtils.initTaxiData(scenario, prtConfig.getVehiclesFile(), prtConfig.getRanksFile());
		context.setVrpData(taxiData);
		
		ModeRoutingParams pars = new ModeRoutingParams();
		pars.setMode(PrtRequestCreator.MODE);
		pars.setBeelineDistanceFactor(1.);
//		pars.setTeleportedModeFreespeedFactor(1.);
		pars.setTeleportedModeSpeed(40.);
		controler.getConfig().plansCalcRoute().addModeRoutingParams(pars);
		
//		PrtTripRouterFactoryImpl factory = new PrtTripRouterFactoryImpl(context, ttime, tdis);
//		factory.instantiateAndConfigureTripRouter(new RoutingContextImpl(tdis, ttime));
		controler.setTripRouterFactory(new PrtTripRouterFactoryImpl(context, ttime, tdis));
		
		TaxiOptimizerConfiguration taxiConfig = initOptimizerConfiguration(prtConfig, context, calculator, algorithmConfig);
		TaxiOptimizer optimizer = algorithmConfig.createTaxiOptimizer(taxiConfig);
		
		controler.setMobsimFactory(new PrtQSimFactory(prtConfig, context, optimizer));
		
	}
	
	private TaxiOptimizerConfiguration initOptimizerConfiguration(PrtConfigGroup prtConfig, MatsimVrpContext context,
			VrpPathCalculator calculator, AlgorithmConfig algorithmConfig){
		
		TaxiSchedulerParams taxiParams = new TaxiSchedulerParams(prtConfig.getDestinationKnown(), prtConfig.getPickupDuration(), prtConfig.getDropoffDuration());
		
		if(prtConfig.getVehicleCapacity() > 1){
			
			PrtScheduler scheduler = new PrtScheduler(context, calculator, taxiParams);
			NPersonsVehicleRequestPathFinder vrpFinder = new NPersonsVehicleRequestPathFinder(calculator, scheduler, prtConfig.getVehicleCapacity());
			FilterFactory filterFactory = new DefaultFilterFactory(scheduler, 0, 0);
			
			return new TaxiOptimizerConfiguration(context, calculator, scheduler, vrpFinder, filterFactory,
					algorithmConfig.getGoal(), prtConfig.getPrtOutputDirectory());
			
		}
		
		TaxiScheduler scheduler = new TaxiScheduler(context, calculator, taxiParams);
		VehicleRequestPathFinder vrpFinder = new VehicleRequestPathFinder(calculator, scheduler);
		FilterFactory filterFactory = new DefaultFilterFactory(scheduler, 0, 0);
		
		return new TaxiOptimizerConfiguration(context, calculator, scheduler, vrpFinder, filterFactory,
				algorithmConfig.getGoal(), prtConfig.getPrtOutputDirectory());
		
	}
	
}
