package playground.dhosse.scenarios.bhls.example;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * 
 * @author dhosse
 *
 */
public class BhlsMain {
	
	public static void main(String args[]){
		
		Config config = ConfigUtils.createConfig();
		
		config.controler().setLastIteration(100);
		config.controler().setOutputDirectory("/home/dhosse/brt/");
		
		StrategySettings expBeta = new StrategySettings();
		expBeta.setStrategyName("ChangeExpBeta");
		expBeta.setSubpopulation(null);
		expBeta.setWeight(0.8);
		config.strategy().addStrategySettings(expBeta);
		
		StrategySettings modeChoice = new StrategySettings();
		modeChoice.setStrategyName("SubtourModeChoice");
		modeChoice.setSubpopulation(null);
		modeChoice.setWeight(0.2);
		modeChoice.setDisableAfter((int)(0.7 * config.controler().getLastIteration()));
		config.strategy().addStrategySettings(modeChoice);
		
		config.transit().setUseTransit(false);
		
		config.qsim().setEndTime(24*3600);
		
		ActivityParams home = new ActivityParams("home");
		home.setTypicalDuration(12 * 3600);
		config.planCalcScore().addActivityParams(home);
		
		ActivityParams work = new ActivityParams("work");
		work.setTypicalDuration(8 * 3600);
		config.planCalcScore().addActivityParams(work);
		
		ModeRoutingParams pars = new ModeRoutingParams("pt");
		pars.setTeleportedModeFreespeedFactor(2.);
		config.plansCalcRoute().addModeRoutingParams(pars);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		CreateExampleNetwork.createYShapedNetwork(scenario);
		new NetworkWriter(scenario.getNetwork()).write("/home/dhosse/network_brt.xml.gz");
		
		CreateExamplePopulation.createAgents(scenario);
		new PopulationWriter(scenario.getPopulation()).write("/home/dhosse/population_brt.xml.gz");
		
		final Controler controler = new Controler(scenario);
		controler.run();
		
	}

}
