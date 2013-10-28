package analyzerExampleScenario;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;

public class RunScenario {

	public static void main(String[] args){
		
		Config config = ConfigUtils.loadConfig("Z:/WinHome/ArbeitWorkspace/Analyzer/input/config_pttutorial.xml");
		config.controler().setWriteEventsInterval(1); //soll eigentlich direkt im controler gehen, laut http://matsim.org/node/624 (8 lessons tutorial 7.1)
		// included in config_ptturial
		
		/*
		ActivityParams work = new ActivityParams("w");
		work.setTypicalDuration(8*60*60);
		config.planCalcScore().addActivityParams(work);
		ActivityParams home = new ActivityParams("h");
		home.setTypicalDuration(14*60*60);
		config.planCalcScore().addActivityParams(home);
		*/
		
		//Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.loadScenario(config);
		//new MatsimNetworkReader(scenario).readFile("./input/network_tut.xml");
		
		Controler controler = new Controler(scenario);

		controler.setOverwriteFiles(true);
		controler.run();
		
	}
	
}
