package playground.gleich.bestTimeIntegration;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;

import besttimeresponseintegration.BestTimeResponseStrategyProvider;
import matsimintegration.TimeDiscretizationInjection;

/**
 * 
 * @author gleich
 *
 */
public class RunCottbusScenarioWithDifferentStrategies {
	
	public static void main (String[] args){
		runSeparateTimeAllocationMutatorAndReRoute();
		runCombinedTimeAllocationMutatorReRoute();
		runBestTimeResponse();
	}
	
	static void runSeparateTimeAllocationMutatorAndReRoute(){
		String configFile = "C:/Users/gleich/ArbeitWorkspace5/Input-data/cottbus-with-pt/config_withoutStrategySet.xml" ;
		Config config = ConfigUtils.loadConfig(configFile);
		config.controler().setOutputDirectory("output/bestTimeIntegration/SeparateTimeAllocationMutatorAndReRoute");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		StrategySettings timeAlloc = new StrategySettings();
		timeAlloc.setStrategyName("TimeAllocationMutator");
		timeAlloc.setWeight(0.1);
		config.strategy().addStrategySettings(timeAlloc);
		StrategySettings reRoute = new StrategySettings();
		reRoute.setStrategyName("ReRoute");
		reRoute.setWeight(0.1);
		config.strategy().addStrategySettings(reRoute);
		StrategySettings changeExpBeta = new StrategySettings();
		changeExpBeta.setStrategyName("ChangeExpBeta");
		changeExpBeta.setWeight(0.8);
		config.strategy().addStrategySettings(changeExpBeta);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		Controler controler = new Controler(scenario);
		controler.run();
	}
	
	static void runCombinedTimeAllocationMutatorReRoute(){
		String configFile = "C:/Users/gleich/ArbeitWorkspace5/Input-data/cottbus-with-pt/config_withoutStrategySet.xml" ;
		Config config = ConfigUtils.loadConfig(configFile);
		config.controler().setOutputDirectory("output/bestTimeIntegration/CombinedTimeAllocationMutatorReRoute");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		StrategySettings timeAllocReRoute = new StrategySettings();
		timeAllocReRoute.setStrategyName("TimeAllocationMutator_ReRoute");
		timeAllocReRoute.setWeight(0.2);
		config.strategy().addStrategySettings(timeAllocReRoute);
		StrategySettings changeExpBeta = new StrategySettings();
		changeExpBeta.setStrategyName("ChangeExpBeta");
		changeExpBeta.setWeight(0.8);
		config.strategy().addStrategySettings(changeExpBeta);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		Controler controler = new Controler(scenario);
		controler.run();
	}
	
	static void runBestTimeResponse(){
		String configFile = "C:/Users/gleich/ArbeitWorkspace5/Input-data/cottbus-with-pt/config_withoutStrategySet.xml" ;
		Config config = ConfigUtils.loadConfig(configFile);
		config.controler().setOutputDirectory("output/bestTimeIntegration/BestTimeResponse");
		String STRATEGY_NAME = "BestTimeResponse";
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		StrategySettings stratSets = new StrategySettings();
		stratSets.setStrategyName(STRATEGY_NAME);
		stratSets.setWeight(0.2);
		config.strategy().addStrategySettings(stratSets);
		
		final Controler controler = new Controler(config);
		
		//add the binding strategy 
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(TimeDiscretizationInjection.class);
				addPlanStrategyBinding(STRATEGY_NAME).toProvider(BestTimeResponseStrategyProvider.class);
			}
		});
		controler.run();
	}

}
