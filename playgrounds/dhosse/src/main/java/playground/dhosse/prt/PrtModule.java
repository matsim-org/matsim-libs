package playground.dhosse.prt;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.MatsimVrpContextImpl;
import org.matsim.contrib.dvrp.router.*;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils.*;
import org.matsim.contrib.dvrp.util.TimeDiscretizer;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizers;
import org.matsim.contrib.taxi.optimizer.AbstractTaxiOptimizerParams.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.controler.*;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.*;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

import com.google.inject.Provider;

import playground.dhosse.prt.data.PrtData;
import playground.dhosse.prt.launch.PrtParameters.AlgorithmConfig;
import playground.dhosse.prt.passenger.PrtRequestCreator;
import playground.dhosse.prt.router.PrtTripRouterFactoryImpl;
import playground.michalm.taxi.data.ETaxiData;
import playground.michalm.taxi.data.file.*;

public class PrtModule {
	
	private MatsimVrpContextImpl context;
	private PrtConfigGroup prtConfig;
	private AlgorithmConfig algorithmConfig;
	private TravelTime ttime;
	private TravelDisutility tdis;
	
	public void configureControler(final Controler controler){
		
		prtConfig = ConfigUtils.addOrGetModule(controler.getConfig(), PrtConfigGroup.GROUP_NAME, PrtConfigGroup.class);
		
		Scenario scenario = ScenarioUtils.loadScenario(controler.getConfig());
		
		algorithmConfig = AlgorithmConfig.valueOf(prtConfig.getAlgorithmConfig());
		
		TravelTimeSource ttimeSource = algorithmConfig.getTravelTimeSource();
		
		ttime = null;
		if(ttimeSource.equals(TravelTimeSource.FREE_FLOW_SPEED)){
			
			ttime = new FreeSpeedTravelTime();
			
		} else{
			
			ttime = VrpLauncherUtils.initTravelTimeCalculatorFromEvents(scenario, prtConfig.getEventsFile(), 15*60).getLinkTravelTimes();
			
		}
		
		tdis = new TimeAsTravelDisutility(ttime);
		
		ModeRoutingParams pars = new ModeRoutingParams();
		pars.setMode(PrtRequestCreator.MODE);
		pars.setTeleportedModeSpeed(1.);
		controler.getConfig().plansCalcRoute().getModeRoutingParams().put(PrtRequestCreator.MODE, pars);
		
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
						return new PrtQSimFactory(prtConfig, context, ttime, tdis, algorithmConfig).createMobsim(controler.getScenario(), controler.getEvents());
					}
				});
			}
		});

		controler.addControlerListener(new PrtControllerListener(prtConfig, controler, context, prtData));
		
	}
	
}
