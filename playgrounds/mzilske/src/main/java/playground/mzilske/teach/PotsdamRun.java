package playground.mzilske.teach;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup.MobsimType;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

public class PotsdamRun implements Runnable {
	
	public static void main(String[] args) {
		PotsdamRun potsdamRun = new PotsdamRun();
		potsdamRun.run();
	}
	
	@Override
	public void run() {
		Config config = ConfigUtils.createConfig();
//		config.vspExperimental().setActivityDurationInterpretation(VspExperimentalConfigGroup.END_TIME_ONLY);
//		config.vspExperimental().setRemovingUnneccessaryPlanAttributes(true);
		config.network().setInputFile("input/potsdam/network.xml");
		config.plans().setInputFile("input/potsdam/plans.xml");
		config.controler().setOutputDirectory("output");
		config.controler().setMobsim(MobsimType.qsim.toString());
		config.strategy().setMaxAgentPlanMemorySize(5);
		ActivityParams workParams = new ActivityParams("work");
		workParams.setTypicalDuration(60*60*8);
		config.planCalcScore().addActivityParams(workParams);
		ActivityParams homeParams = new ActivityParams("home");
		homeParams.setTypicalDuration(16*60*60);
		config.planCalcScore().addActivityParams(homeParams);
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(0);
		StrategySettings stratSets = new StrategySettings(Id.create("1", StrategySettings.class));
		stratSets.setStrategyName("ReRoute");
		stratSets.setWeight(0.2);
		stratSets.setDisableAfter(8);
		StrategySettings expBeta = new StrategySettings(Id.create("2", StrategySettings.class));
		expBeta.setStrategyName("ChangeExpBeta");
		expBeta.setWeight(0.6);

		
		config.strategy().addStrategySettings(expBeta);
		config.strategy().addStrategySettings(stratSets);
		
		QSimConfigGroup tmp = config.qsim();
		tmp.setFlowCapFactor(100);
		tmp.setStorageCapFactor(100);
		tmp.setRemoveStuckVehicles(false);
		tmp.setEndTime(24*60*60);
		
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		config.global().setRandomSeed(37);
		new MatsimNetworkReader(scenario).readFile("input/potsdam/network.xml");
		PotsdamPop potsdamPop = new PotsdamPop(scenario, config.global().getRandomSeed());
		potsdamPop.run();
		
		
		
		

		config.controler().setOutputDirectory("output-homogeneous-37");
		
		final Controler controller = new Controler(scenario);
		controller.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		controller.run();
		
//		Link bridge1 = scenario.getNetwork().getLinks().get(Id.create(17919, Link.class));
//		bridge1.setCapacity(0);
//		bridge1.setFreespeed(0);
//		
//		Link bridge2 = scenario.getNetwork().getLinks().get(Id.create(1388, Link.class));
//		bridge2.setCapacity(0);
//		bridge2.setFreespeed(0);
//		
//		stratSets.setDisableAfter(18);
//		config.controler().setFirstIteration(11);
//		config.controler().setLastIteration(20);
//		config.controler().setOutputDirectory("outputBridgeClosed");
//		
//		
//		final Controler controlerAfterMeasure = new Controler(scenario);
//		controlerAfterMeasure.setOverwriteFiles(true);
//		controlerAfterMeasure.run();
	}
}