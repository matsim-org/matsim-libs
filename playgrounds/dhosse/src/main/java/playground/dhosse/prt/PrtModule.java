package playground.dhosse.prt;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.MatsimVrpContextImpl;
import org.matsim.contrib.dvrp.router.LeastCostPathCalculatorWithCache;
import org.matsim.contrib.dvrp.router.VrpPathCalculator;
import org.matsim.contrib.dvrp.router.VrpPathCalculatorImpl;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils.TravelDisutilitySource;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils.TravelTimeSource;
import org.matsim.contrib.dvrp.util.time.TimeDiscretizer;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.RoutingContextImpl;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import playground.dhosse.prt.launch.PrtParameters.AlgorithmConfig;
import playground.dhosse.prt.passenger.PrtRequestCreator;
import playground.dhosse.prt.request.NPersonsVehicleRequestPathFinder;
import playground.dhosse.prt.router.PrtTripRouterFactoryImpl;
import playground.dhosse.prt.scheduler.PrtScheduler;
import playground.michalm.taxi.data.TaxiData;
import playground.michalm.taxi.optimizer.TaxiOptimizer;
import playground.michalm.taxi.optimizer.TaxiOptimizerConfiguration;
import playground.michalm.taxi.optimizer.filter.DefaultFilterFactory;
import playground.michalm.taxi.optimizer.filter.FilterFactory;
import playground.michalm.taxi.run.TaxiLauncherUtils;
import playground.michalm.taxi.scheduler.TaxiScheduler;
import playground.michalm.taxi.scheduler.TaxiSchedulerParams;
import playground.michalm.taxi.vehreqpath.VehicleRequestPathFinder;

public class PrtModule {
	
	public void configureControler(Controler controler){
		
		PrtConfigGroup prtConfig = ConfigUtils.addOrGetModule(controler.getConfig(), PrtConfigGroup.GROUP_NAME, PrtConfigGroup.class);
		
		Scenario scenario = VrpLauncherUtils.initScenario(controler.getConfig().network().getInputFile(),
				controler.getConfig().plans().getInputFile(), prtConfig.getEventsFile());
		
		AlgorithmConfig algorithmConfig = AlgorithmConfig.valueOf(prtConfig.getAlgorithmConfig());
		
		TravelTimeSource ttimeSource = algorithmConfig.getTravelTimeSource();
		TravelDisutilitySource tdisSource = algorithmConfig.getTravelDisutilitySource();
		
		TravelTime ttime = VrpLauncherUtils.initTravelTime(scenario, ttimeSource, prtConfig.getEventsFile());
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
