package org.matsim.contrib.freight.usecases.chessboard;

import org.matsim.api.core.v01.Id;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;

public class RunPassengerPlansFromScratch {
	
	public static void main(String[] args) {
		String NETWORK_FILENAME = "input/usecases/chessboard/network/grid9x9.xml";
		String PLANS_FILENAME = "input/usecases/chessboard/passenger/passengerPlans.xml";
		Config config = new Config();
		config.addCoreModules();
		
		ActivityParams workParams = new ActivityParams("work");
		workParams.setTypicalDuration(60 * 60 * 8);
		config.planCalcScore().addActivityParams(workParams);
		ActivityParams homeParams = new ActivityParams("home");
		homeParams.setTypicalDuration(16 * 60 * 60);
		config.planCalcScore().addActivityParams(homeParams);
		config.global().setCoordinateSystem("EPSG:32632");
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(2);
		config.network().setInputFile(NETWORK_FILENAME);
		config.plans().setInputFile(PLANS_FILENAME);
		
		StrategySettings bestScore = new StrategySettings(Id.create("1", StrategySettings.class));
		bestScore.setStrategyName("BestScore");
		bestScore.setWeight(0.5);
		
		StrategySettings reRoute = new StrategySettings(Id.create("2", StrategySettings.class));
		reRoute.setStrategyName("ReRoute");
		reRoute.setWeight(0.5);
//		reRoute.setDisableAfter(300);
		
		config.strategy().setMaxAgentPlanMemorySize(5);
		config.strategy().addStrategySettings(bestScore);
		config.strategy().addStrategySettings(reRoute);
//		
		Controler controler = new Controler(config);
		controler.getConfig().controler().setWriteEventsInterval(1);
        controler.getConfig().controler().setCreateGraphs(false);
        controler.setOverwriteFiles(true);
		
		controler.run();

	}

}
