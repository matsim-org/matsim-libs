package playground.dhosse.gap.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
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
import playground.dhosse.gap.scenario.network.NetworkCreator;

public class GAPScenarioRunner {

	private static final String inputPath = Global.matsimDir + "INPUT/";
	private static final String simInputPath = Global.matsimDir + "OUTPUT/" + Global.runID +"/input/";
	private static final String outputPath = "/run/user/1009/gvfs/smb-share:server=innoz-dc01,share=innoz/2_MediengestützteMobilität/10_Projekte/eGAP/30_Modellierung/OUTPUT/" + Global.runID + "/ouput_/";
	
	/**
	 * edit the static method executed in the main method
	 * to run a different case
	 * 
	 * @param args
	 */
	public static void main(String args[]){
		
//		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
//		new MatsimNetworkReader(scenario).readFile("/home/dhosse/merged-networkV2_20150929.xml");
//		NetworkCreator.createAdditionalLinks2025(scenario.getNetwork(), "/home/dhosse/merged-network_2025.xml.gz");
		
//		runBaseCaseRouteChoice();
		runBaseCaseRouteChoiceAndModeChoice();
//		runAnalysis();
		
	}

	/**
	 * Runs the base scenario with route choice as only innovative strategy.
	 */
	private static void runBaseCaseRouteChoice() {
		
		//basics: load config settings and scenario
		Config config = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(config, simInputPath + "config.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		Scenario scenario2 = ScenarioUtils.loadScenario(config);
		new NetworkCleaner().run(scenario2.getNetwork());
		
		XY2Links xy2links = new XY2Links(scenario2);
		for(Person person : scenario.getPopulation().getPersons().values()){
			xy2links.run(person);
		}
		
		//after everything else is set up, start the simulation
		final Controler controler = new Controler(scenario);
		
		controler.run();
		
	}
	
	/**
	 * Runs the base scenario with route choice and mode choice as innovative strategies.
	 */
	private static void runBaseCaseRouteChoiceAndModeChoice(){
		
		final Config config = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(config, simInputPath + "config.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		Scenario scenario2 = ScenarioUtils.loadScenario(config);
		new NetworkCleaner().run(scenario2.getNetwork());
		
		XY2Links xy2links = new XY2Links(scenario2);
		for(Person person : scenario.getPopulation().getPersons().values()){
			xy2links.run(person);
		}
		
		final Controler controler = new Controler(scenario);
		
		addModeChoiceStrategyModules(controler);
		
		controler.getEvents().addHandler(new ZugspitzbahnFareHandler(controler));
		
		controler.run();
		
	}
	
	/**
	 * Adds subtour mode choice strategy settings to the controler.
	 * These strategies are configured for different types of subpopulations (persons with car and license, commuters, persons without license).
	 * 
	 * @param controler
	 */
	private static void addModeChoiceStrategyModules(final Controler controler) {

		StrategySettings carAvail = new StrategySettings();
		carAvail.setStrategyName("SubtourModeChoice_".concat(Global.GP_CAR));
		carAvail.setSubpopulation(Global.GP_CAR);
		carAvail.setWeight(0.1);
		controler.getConfig().strategy().addStrategySettings(carAvail);
		
		StrategySettings nonCarAvail = new StrategySettings();
		nonCarAvail.setStrategyName("SubtourModeChoice_".concat(Global.NO_CAR));
		nonCarAvail.setSubpopulation(null);
		nonCarAvail.setWeight(0.1);
		controler.getConfig().strategy().addStrategySettings(nonCarAvail);
		
		StrategySettings commuter = new StrategySettings();
		commuter.setStrategyName("SubtourModeChoice_".concat(Global.COMMUTER));
		commuter.setSubpopulation(Global.COMMUTER);
		commuter.setWeight(0.1);
		controler.getConfig().strategy().addStrategySettings(commuter);
		
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
				addPlanStrategyBinding("SubtourModeChoice_".concat(Global.NO_CAR)).toProvider(new javax.inject.Provider<PlanStrategy>() {
					String[] availableModes = {TransportMode.pt, TransportMode.bike, TransportMode.walk};
					String[] chainBasedModes = {TransportMode.bike};

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
	
//		PersonAnalysis.createLegModeDistanceDistribution("/home/dhosse/plansV3_cleaned.xml.gz", "/home/danielhosse/Dokumente/lmdd/");
		SpatialAnalysis.writePopulationToShape("/home/dhosse/plansV3_cleaned.xml.gz", "/home/dhosse/Dokumente/01_eGAP/popV3.shp");
	
	}
	
}
