package playground.gleich.analyzer.exampleScenario;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;

public class RunScenario {
	
	private static String pathToExampleScenario = "Z:/WinHome/ArbeitWorkspace/Analyzer/";

	public static void main(String[] args){
		
		//"input/ExampleScenario/config_pttutorial_withoutPaths.xml"
		
		Config config = ConfigUtils.loadConfig(pathToExampleScenario 
				+ "input/config_exampleScenario.xml");
		config.network().setInputFile(pathToExampleScenario 
				+ "input/network.xml");
		config.transit().setTransitScheduleFile(pathToExampleScenario 
				+ "input/transitSchedule.xml");
		config.transit().setVehiclesFile(pathToExampleScenario 
				+ "input/Vehicles.xml");
		config.plans().setInputFile(pathToExampleScenario 
				+ "input/ijkl_plans.xml");
		config.controler().setOutputDirectory(pathToExampleScenario 
				+ "output/testOneBusManyIterations");
		config.controler().setWriteEventsInterval(1); //soll eigentlich direkt im controler gehen, laut http://matsim.org/node/624 (8 lessons tutorial 7.1)
		// included in config_ptturial
		
		config.controler().setLastIteration(20);
		
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

		controler.getConfig().controler().setOverwriteFileSetting(
				false ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );

		controler.run();
		
	}
	
}
