package playground.gleich.bestTimeIntegration;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
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
//		runSeparateTimeAllocationMutatorAndReRoute();
//		runCombinedTimeAllocationMutatorReRoute();
		runBestTimeResponse();
	}
	
	static void runSeparateTimeAllocationMutatorAndReRoute(){
//		String configFile = "C:/Users/gleich/ArbeitWorkspace5/Input-data/cottbus-with-pt/config_withoutStrategySet.xml" ;
		String configFile = "C:/Users/gleich/ArbeitWorkspace5/matsim/matsim/examples/scenarios/pt-tutorial/0.config.xml" ;
		Config config = ConfigUtils.loadConfig(configFile);
//		config.controler().setOutputDirectory("output/bestTimeIntegration/SeparateTimeAllocationMutatorAndReRoute");
		config.controler().setOutputDirectory("output/bestTimeIntegration/pt-tutorial/SeparateTimeAllocationMutatorAndReRoute");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(100);
		
		config.strategy().clearStrategySettings();
		
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
		
		config.strategy().setFractionOfIterationsToDisableInnovation(0.8);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		Controler controler = new Controler(scenario);
		controler.run();
	}
	
	static void runCombinedTimeAllocationMutatorReRoute(){
//		String configFile = "C:/Users/gleich/ArbeitWorkspace5/Input-data/cottbus-with-pt/config_withoutStrategySet.xml" ;
		String configFile = "C:/Users/gleich/ArbeitWorkspace5/matsim/matsim/examples/scenarios/pt-tutorial/0.config.xml" ;
		Config config = ConfigUtils.loadConfig(configFile);
//		config.controler().setOutputDirectory("output/bestTimeIntegration/CombinedTimeAllocationMutatorReRoute");
		config.controler().setOutputDirectory("output/bestTimeIntegration/pt-tutorial/CombinedTimeAllocationMutatorReRoute");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(100);
		
		config.strategy().clearStrategySettings();
		
		StrategySettings timeAllocReRoute = new StrategySettings();
		timeAllocReRoute.setStrategyName("TimeAllocationMutator_ReRoute");
		timeAllocReRoute.setWeight(0.2);
		config.strategy().addStrategySettings(timeAllocReRoute);
		StrategySettings changeExpBeta = new StrategySettings();
		changeExpBeta.setStrategyName("ChangeExpBeta");
		changeExpBeta.setWeight(0.8);
		config.strategy().addStrategySettings(changeExpBeta);
		
		config.strategy().setFractionOfIterationsToDisableInnovation(0.8);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		Controler controler = new Controler(scenario);
		controler.run();
	}
	
	static void runBestTimeResponse(){
		/*
		 *  Use restrictions of BestTimeResponse strategy:
		 *  - number of (real) activities > 1
		 *  - zeroUtility activity duration >= 0.1 s -> e.g. 5 min duration kindergarten1 activity causes exception
		 *  - pt lead to crashes prior to 28-11-2016 modifications in BestTimeResponseTravelTimes and BestTimeResponseStrategyFunctionality
		 *  - Cottbus scenario gives no console output for more than 2h after replanning starts for the first time
		 */
		
//		String configFile = "C:/Users/gleich/ArbeitWorkspace5/Input-data/cottbus-with-pt/config_withoutStrategySet.xml" ;
		String configFile = "C:/Users/gleich/ArbeitWorkspace5/matsim/matsim/examples/scenarios/pt-tutorial/0.config.xml" ;

		Config config = ConfigUtils.loadConfig(configFile);
//		config.controler().setOutputDirectory("output/bestTimeIntegration/BestTimeResponse");
		config.controler().setOutputDirectory("output/bestTimeIntegration/pt-tutorial/BestTimeResponse");
		
//		config.controler().setLastIteration(10);
		
		config.controler().setLastIteration(100);

		for(ActivityParams actParam: config.planCalcScore().getActivityParams()){
			if(actParam.getTypicalDuration() < 2*60*60){
				actParam.setTypicalDuration(2*60*60);
			}
		}
		
		config.strategy().clearStrategySettings();
		
		String STRATEGY_NAME = "BestTimeResponse";
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		StrategySettings stratSets = new StrategySettings();
		stratSets.setStrategyName(STRATEGY_NAME);
		stratSets.setWeight(0.2);
		config.strategy().addStrategySettings(stratSets);
		StrategySettings changeExpBeta = new StrategySettings();
		changeExpBeta.setStrategyName("ChangeExpBeta");
		changeExpBeta.setWeight(0.8);
		config.strategy().addStrategySettings(changeExpBeta);
		
		config.strategy().setFractionOfIterationsToDisableInnovation(0.8);
		
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
