package playground.artemc.siouxFalls;


import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.scenario.ScenarioUtils;

import playground.artemc.socialCost.MeanTravelTimeCalculator;



public class MyControllerMethana {
	
	private static String input;
	private static String output;
	
	/**
	 * @param args
	 */
	public static void main(String[] args){
		
		input = args[0];
		output = args[1];

		Controler controler = null;
		Scenario scenario = initSampleScenario();
		controler = new Controler(scenario);

		
		/*
		 * Scoring also has to take the social costs into account.
		 * This cannot be moved to the initializer since the scoring functions
		 * are created even before the startup event is created.
		 */ 
	//	controler.setScoringFunctionFactory(new TimeAndMoneyDependentScoringFunctionFactory());
		
		Initializer initializer = new Initializer();
		controler.addControlerListener(initializer);
		//controler.addControlerListener(new CalibrationStatsListener(controler.getEvents(), SURVEY_FILES,1, "HITS 2008", "Red_Scheme"));
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		controler.run();

	}
	
//	private static Scenario initSampleScenario() {
//
//		Config config = ConfigUtils.loadConfig("scenarios/siouxFalls/config_car.xml");
//		//Config config = ConfigUtils.loadConfig("scenarios/siouxFalls/config_car.xml");
//		//config.controler().setOutputDirectory("./outpout_SiouxFalls/pt_UE_7200_calibration_PT_6min_600m");
//		//config.controler().setOutputDirectory("./outpout_SiouxFalls/SiouxFalls_5PT_Lines");
//		//config.controler().setOutputDirectory("H:/SiouxFallsOutput_OnlyCar_40848_500it");
//		//config.controler().setLastIteration(500);
//		Scenario scenario = ScenarioUtils.loadScenario(config);
//		
//		return scenario;
//	}
	
	
	private static Scenario initSampleScenario() {

		Config config = ConfigUtils.loadConfig(input+"config.xml");
		config.controler().setOutputDirectory(output);
		config.network().setInputFile(input+"network.xml");
		config.plans().setInputFile(input+"population.xml");
		config.transit().setTransitScheduleFile(input+"transitSchedule.xml");
		config.transit().setVehiclesFile(input+"vehicles.xml");
		config.controler().setOutputDirectory(output);
		
		//		config.controler().setLastIteration(10);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		return scenario;
	}
	
	private static class Initializer implements StartupListener {

		@Override
		public void notifyStartup(StartupEvent event) {
			Controler controler = event.getControler();
			// create a plot containing the mean travel times
			Set<String> transportModes = new HashSet<String>();
			transportModes.add(TransportMode.car);
			transportModes.add(TransportMode.pt);
			transportModes.add(TransportMode.walk);
			MeanTravelTimeCalculator mttc = new MeanTravelTimeCalculator(controler.getScenario(), transportModes);
			controler.addControlerListener(mttc);
			controler.getEvents().addHandler(mttc);
		}
	}

}
