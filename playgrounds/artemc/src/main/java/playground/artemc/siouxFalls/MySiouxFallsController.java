package playground.artemc.siouxFalls;


import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.scenario.ScenarioUtils;

import playground.artemc.calibration.CalibrationStatsListener;
import playground.artemc.socialCost.MeanTravelTimeCalculator;


public class MySiouxFallsController {

	private static final String[] SURVEY_FILES = {"C:/Workspace/roadpricingSingapore/input/distanceByModeSurvey.csv", "C:/Workspace/roadpricingSingapore/input/travelTimeByModeSurvey.csv"};
	/**
	 * @param args
	 */
	public static void main(String[] args){
		
		Controler controler = null;
		if (args.length == 0) {
			controler = new Controler(initSampleScenario());
		} else controler = new Controler(args);
		
		/*
		 * Scoring also has to take the social costs into account.
		 * This cannot be moved to the initializer since the scoring functions
		 * are created even before the startup event is created.
		 */ 
	//	controler.setScoringFunctionFactory(new TimeAndMoneyDependentScoringFunctionFactory());
		
		Initializer initializer = new Initializer();
		controler.addControlerListener(initializer);
		Set<Id<Person>> pIdsToExclude = new HashSet<Id<Person>>();
		controler.addControlerListener(new CalibrationStatsListener(controler.getEvents(), SURVEY_FILES,1, "HITS 2008", "Red_Scheme", pIdsToExclude));
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		controler.run();

	}
	
	private static Scenario initSampleScenario() {

		Config config = ConfigUtils.loadConfig("C:/Workspace/roadpricingSingapore/scenarios/siouxFalls/config.xml");
		//config.controler().setOutputDirectory("./outpout_SiouxFalls/pt_UE_7200_calibration_PT_6min_600m");
		//config.controler().setOutputDirectory("./outpout_SiouxFalls/SiouxFalls_5PT_Lines");
		config.controler().setOutputDirectory("C:/Workspace/roadpricingSingapore/output_SiouxFalls/sf_10it_withPT");
		config.controler().setLastIteration(10);
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
			MeanTravelTimeCalculator mttc = new MeanTravelTimeCalculator(controler.getScenario(), transportModes);
			controler.addControlerListener(mttc);
			controler.getEvents().addHandler(mttc);
		}
	}

}
