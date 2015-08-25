package playground.dhosse.prt;

import com.google.inject.Provider;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.MatsimVrpContextImpl;
import org.matsim.contrib.dvrp.router.LeastCostPathCalculatorWithCache;
import org.matsim.contrib.dvrp.router.VrpPathCalculator;
import org.matsim.contrib.dvrp.router.VrpPathCalculatorImpl;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils.TravelDisutilitySource;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils.TravelTimeSource;
import org.matsim.contrib.dvrp.util.TimeDiscretizer;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

import playground.dhosse.prt.data.PrtData;
import playground.dhosse.prt.launch.PrtParameters.AlgorithmConfig;
import playground.dhosse.prt.passenger.PrtRequestCreator;
import playground.dhosse.prt.router.PrtTripRouterFactoryImpl;
import playground.michalm.taxi.data.ETaxiData;
import playground.michalm.taxi.data.file.ETaxiReader;
import playground.michalm.taxi.data.file.TaxiRankReader;

public class PrtModule {
	
	private MatsimVrpContextImpl context;
	private PrtConfigGroup prtConfig;
	private AlgorithmConfig algorithmConfig;
	private TravelTime ttime;
	private TravelDisutility tdis;
	private VrpPathCalculator calculator;
	
	public void configureControler(final Controler controler){
		
		prtConfig = ConfigUtils.addOrGetModule(controler.getConfig(), PrtConfigGroup.GROUP_NAME, PrtConfigGroup.class);
		
		Scenario scenario = ScenarioUtils.loadScenario(controler.getConfig());
		
		algorithmConfig = AlgorithmConfig.valueOf(prtConfig.getAlgorithmConfig());
		
		TravelTimeSource ttimeSource = algorithmConfig.getTravelTimeSource();
		TravelDisutilitySource tdisSource = algorithmConfig.getTravelDisutilitySource();
		
		ttime = null;
		if(ttimeSource.equals(TravelTimeSource.FREE_FLOW_SPEED)){
			
			ttime = new FreeSpeedTravelTime();
			
		} else{
			
			ttime = VrpLauncherUtils.initTravelTimeCalculatorFromEvents(scenario, prtConfig.getEventsFile(), 15*60).getLinkTravelTimes();
			
		}
		
		tdis = VrpLauncherUtils.initTravelDisutility(tdisSource, ttime);
		
		ModeRoutingParams pars = new ModeRoutingParams();
		pars.setMode(PrtRequestCreator.MODE);
		pars.setTeleportedModeSpeed(1.);
		controler.getConfig().plansCalcRoute().getModeRoutingParams().put(PrtRequestCreator.MODE, pars);
		
		LeastCostPathCalculator router = new Dijkstra(scenario.getNetwork(), tdis, ttime);
		LeastCostPathCalculatorWithCache routerWithCache = new LeastCostPathCalculatorWithCache(router, new TimeDiscretizer(30*4, 15, false));
		
		calculator = new VrpPathCalculatorImpl(routerWithCache, ttime, tdis);
		
		context = new MatsimVrpContextImpl();
		context.setScenario(scenario);
		
		ETaxiData data = new ETaxiData();
		context.setVrpData(data);
		new TaxiRankReader(context.getScenario(), (ETaxiData) context.getVrpData()).parse(prtConfig.getRanksFile());
		new ETaxiReader(scenario, data).parse(prtConfig.getVehiclesFile());
		
		PrtData prtData = new PrtData(scenario.getNetwork(), data);
		
		PrtTripRouterFactoryImpl factory = new PrtTripRouterFactoryImpl(context, ttime, tdis, prtData);
		controler.setTripRouterFactory(factory);

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider(new Provider<Mobsim>() {
					@Override
					public Mobsim get() {
						return new PrtQSimFactory(prtConfig, context, calculator, algorithmConfig).createMobsim(controler.getScenario(), controler.getEvents());
					}
				});
			}
		});

		controler.addControlerListener(new PrtControllerListener(prtConfig, controler, context, prtData));
		
	}
	
}
