package playground.mzilske.teach;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.SimulationConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

public class PotsdamRun implements Runnable {
	
	public static void main(String[] args) {
		PotsdamRun potsdamRun = new PotsdamRun();
		potsdamRun.run();
	}
	
	@Override
	public void run() {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile("input/network.xml");
		config.plans().setInputFile("input/plans.xml");
		config.controler().setOutputDirectory("output");
		ActivityParams workParams = new ActivityParams("work");
		workParams.setTypicalDuration(60*60*8);
		config.planCalcScore().addActivityParams(workParams);
		ActivityParams homeParams = new ActivityParams("home");
		homeParams.setTypicalDuration(16*60*60);
		config.planCalcScore().addActivityParams(homeParams);
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(10);
		StrategySettings stratSets = new StrategySettings(new IdImpl("1"));
		stratSets.setModuleName("ReRoute");
		stratSets.setProbability(0.2);
		stratSets.setDisableAfter(8);
		StrategySettings expBeta = new StrategySettings(new IdImpl("2"));
		expBeta.setModuleName("ChangeExpBeta");
		expBeta.setProbability(0.6);
		config.strategy().addStrategySettings(expBeta);
		config.strategy().addStrategySettings(stratSets);
		SimulationConfigGroup tmp = new SimulationConfigGroup();
		tmp.setFlowCapFactor(0.01);
		tmp.setStorageCapFactor(0.01);
		tmp.setRemoveStuckVehicles(false);
		// tmp.setSnapshotFormat("otfvis");
		tmp.setSnapshotPeriod(60);
		tmp.setEndTime(24*60*60);
		config.addSimulationConfigGroup(tmp);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		
		final Controler controller = new Controler(scenario);
		controller.setOverwriteFiles(true);
		controller.run();
		
		Link bridge1 = scenario.getNetwork().getLinks().get(new IdImpl(17919));
		bridge1.setCapacity(0);
		bridge1.setFreespeed(0);
		
		Link bridge2 = scenario.getNetwork().getLinks().get(new IdImpl(1388));
		bridge2.setCapacity(0);
		bridge2.setFreespeed(0);
		
		stratSets.setDisableAfter(18);
		config.controler().setFirstIteration(11);
		config.controler().setLastIteration(20);
		config.controler().setOutputDirectory("outputBridgeClosed");
		
		
		final Controler controlerAfterMeasure = new Controler(scenario);
		controlerAfterMeasure.setOverwriteFiles(true);
		controlerAfterMeasure.run();
	}
}