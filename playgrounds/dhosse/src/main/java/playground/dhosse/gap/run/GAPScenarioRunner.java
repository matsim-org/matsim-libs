package playground.dhosse.gap.run;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.carsharing.config.CarsharingConfigGroup;
import org.matsim.contrib.carsharing.control.listeners.CarsharingListener;
import org.matsim.contrib.carsharing.qsim.CarsharingQsimFactory;
import org.matsim.contrib.carsharing.replanning.CarsharingSubtourModeChoiceStrategy;
import org.matsim.contrib.carsharing.replanning.RandomTripToCarsharingStrategy;
import org.matsim.contrib.carsharing.runExample.CarsharingUtils;
import org.matsim.contrib.carsharing.scoring.CarsharingScoringFunctionFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl.Builder;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.population.algorithms.XY2Links;

import playground.dhosse.gap.Global;
import playground.dhosse.gap.analysis.SpatialAnalysis;

public class GAPScenarioRunner {

	private static final String inputPath = Global.matsimDir + "INPUT/";
	private static final String simInputPath = Global.matsimDir + "OUTPUT/" + Global.runID +"/input/";
	private static final String outputPath = "/run/user/1007/gvfs/smb-share:server=innoz-dc01,share=innoz/2_MediengestützteMobilität/10_Projekte/eGAP/30_Modellierung/OUTPUT/" + Global.runID + "/ouput_/";
	
	/**
	 * edit the static method executed in the main method
	 * to run a different case
	 * 
	 * @param args
	 */
	public static void main(String args[]){

//		runBaseCaseRouteChoiceOnly();
//		runBaseCaseRouteChoiceAndModeChoice();
		runAnalysis();
//		GeometryUtils.readPolygonFile(Global.dataDir + "Netzwerk/garmisch.poly");
		
	}

	/**
	 * Runs the base scenario with route choice as only innovative strategy.
	 */
	private static void runBaseCaseRouteChoiceOnly() {
		
		//basics: load config settings and scenario
		Config config = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(config, simInputPath + "config.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
//		System.out.println(scenario.getNetwork().getLinks().containsKey(Id.createLinkId("21411")));
		
		//create a second scenario, containing only the road network (rail network will be removed by network cleaner)
		Scenario s2 = ScenarioUtils.loadScenario(config);
		new NetworkCleaner().run(s2.getNetwork());
		
		//this is done in order to set the agent population on links
		//that is the reason why the rail network had to be removed since car agents can't move on rails...
		XY2Links xy2Links = new XY2Links(s2);
		
		for(Person person : scenario.getPopulation().getPersons().values()){
			
			xy2Links.run(person);
			
		}
		
		//after everything else is set up, start the simulation
		final Controler controler = new Controler(scenario);
		
//		System.out.println(scenario.getNetwork().getLinks().containsKey(Id.createLinkId("21411")));
		
		controler.run();
		
	}
	
	/**
	 * Runs the base scenario with route choice and mode choice as innovative strategies.
	 */
	private static void runBaseCaseRouteChoiceAndModeChoice(){
		
		Config config = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(config, simInputPath + "config.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		Scenario s2 = ScenarioUtils.loadScenario(config);
		new NetworkCleaner().run(s2.getNetwork());
		
		XY2Links xy2Links = new XY2Links(s2);
		
		for(Person person : scenario.getPopulation().getPersons().values()){
			
			xy2Links.run(person);
			
		}
		
		final Controler controler = new Controler(scenario);
		
		addModeChoiceStrategyModules(controler);
		
		controler.run();
		
	}
	
	private static void runBaseCaseWithCarSharing(){
		
		Config config = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(config, simInputPath + "config.xml");
		
		CarsharingUtils.addConfigModules(config);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		final Controler controler = new Controler(scenario);
		
		controler.addOverridingModule(new AbstractModule() {
			
			@Override
			public void install() {
				addPlanStrategyBinding("SubtourModeChoice_".concat(Global.GP_CAR)).toProvider(new javax.inject.Provider<PlanStrategy>() {
					String[] availableModes = {TransportMode.car, TransportMode.pt, TransportMode.bike, TransportMode.walk};
					String[] chainBasedModes = {TransportMode.car, TransportMode.bike};

					@Override
					public PlanStrategy get() {
						final Builder builder = new Builder(new RandomPlanSelector<Plan, Person>());
						builder.addStrategyModule(new SubtourModeChoice(controler.getConfig().global().getNumberOfThreads(), availableModes, chainBasedModes, false));
						builder.addStrategyModule(new ReRoute(controler.getScenario()));
						return builder.build();
					}
				});
				
			}
		});
		
		controler.addOverridingModule(new AbstractModule() {
			
			@Override
			public void install() {
				addPlanStrategyBinding("SubtourModeChoice_".concat(Global.COMMUTER)).toProvider(new javax.inject.Provider<PlanStrategy>() {
					String[] availableModes = {TransportMode.car, TransportMode.pt};
					String[] chainBasedModes = {TransportMode.car, TransportMode.bike};

					@Override
					public PlanStrategy get() {
						final Builder builder = new Builder(new RandomPlanSelector<Plan, Person>());
						builder.addStrategyModule(new SubtourModeChoice(controler.getConfig().global().getNumberOfThreads(), availableModes, chainBasedModes, false));
						builder.addStrategyModule(new ReRoute(controler.getScenario()));
						return builder.build();
					}
				});
				
			}
		});
		
		controler.addOverridingModule(new AbstractModule() {
			
			@Override
			public void install() {

				this.addPlanStrategyBinding("RandomTripToCarSharingStrategy").to(RandomTripToCarsharingStrategy.class);
				this.addPlanStrategyBinding("CarsharingSubtourModeChoiceStrategy").to(CarsharingSubtourModeChoiceStrategy.class);
				
			}
		});
		
		controler.addOverridingModule(new AbstractModule() {
			
			@Override
			public void install() {
				
				bindMobsim().toProvider(CarsharingQsimFactory.class);
				
			}
		});
		
		controler.setTripRouterFactory(CarsharingUtils.createTripRouterFactory(scenario));
		
		controler.setScoringFunctionFactory(new CarsharingScoringFunctionFactory( config, scenario.getNetwork()));

		final CarsharingConfigGroup csConfig = (CarsharingConfigGroup) controler.getConfig().getModule(CarsharingConfigGroup.GROUP_NAME);
		controler.addControlerListener(new CarsharingListener(controler,
				csConfig.getStatsWriterFrequency() ) ) ;
		
		controler.run();
		
	}
	
	/**
	 * Adds subtour mode choice strategy settings to the controler.
	 * These strategies are configured for different types of subpopulations (persons with car and license, commuters, persons without license).
	 * 
	 * @param controler
	 */
	private static void addModeChoiceStrategyModules(final Controler controler) {

		controler.addOverridingModule(new AbstractModule() {
			
			@Override
			public void install() {
				addPlanStrategyBinding("SubtourModeChoice_".concat(Global.GP_CAR)).toProvider(new javax.inject.Provider<PlanStrategy>() {
					String[] availableModes = {TransportMode.car, TransportMode.pt, TransportMode.bike, TransportMode.walk};
					String[] chainBasedModes = {TransportMode.car, TransportMode.bike};

					@Override
					public PlanStrategy get() {
						final Builder builder = new Builder(new RandomPlanSelector<Plan, Person>());
						builder.addStrategyModule(new SubtourModeChoice(controler.getConfig().global().getNumberOfThreads(), availableModes, chainBasedModes, false));
						builder.addStrategyModule(new ReRoute(controler.getScenario()));
						return builder.build();
					}
				});
				
			}
		});
		
		controler.addOverridingModule(new AbstractModule() {
			
			@Override
			public void install() {
				addPlanStrategyBinding("SubtourModeChoice_".concat(Global.COMMUTER)).toProvider(new javax.inject.Provider<PlanStrategy>() {
					String[] availableModes = {TransportMode.car, TransportMode.pt};
					String[] chainBasedModes = {TransportMode.car, TransportMode.bike};

					@Override
					public PlanStrategy get() {
						final Builder builder = new Builder(new RandomPlanSelector<Plan, Person>());
						builder.addStrategyModule(new SubtourModeChoice(controler.getConfig().global().getNumberOfThreads(), availableModes, chainBasedModes, false));
						builder.addStrategyModule(new ReRoute(controler.getScenario()));
						return builder.build();
					}
				});
				
			}
		});
		
	}
	
	private static void runAnalysis() {
		
		SpatialAnalysis.writePopulationToShape(inputPath + "Pläne/plansV2.xml.gz", "/home/danielhosse/Dokumente/eGAP/popV2.shp");
	
	}
	
}
