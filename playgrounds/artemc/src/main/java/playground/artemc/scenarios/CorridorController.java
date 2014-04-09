package playground.artemc.scenarios;


import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.artemc.scoreAnalyzer.DisaggregatedCharyparNagelScoringFunctionFactory;
import playground.artemc.scoreAnalyzer.DisaggregatedScoreAnalyzer;
import playground.artemc.socialCost.MeanTravelTimeCalculator;



public class CorridorController {

	/**
	 * @param args
	 */
	public static void main(String[] args){
		
		Controler controler = null;
		Scenario scenario = initSampleScenario();
		if (args.length == 0) {
			controler = new Controler(scenario);
		} else controler = new Controler(args);
				

	//	controler.setScoringFunctionFactory(new TimeAndMoneyDependentScoringFunctionFactory());
		
		Initializer initializer = new Initializer();
		controler.addControlerListener(initializer);
		controler.setOverwriteFiles(true);
		
		ScenarioImpl scnearioImpl = (ScenarioImpl) controler.getScenario();
		controler.setScoringFunctionFactory(new DisaggregatedCharyparNagelScoringFunctionFactory(controler.getConfig().planCalcScore(), controler.getNetwork()));
		controler.addControlerListener(new DisaggregatedScoreAnalyzer(scnearioImpl));

		Logger root = Logger.getRootLogger();
		root.setLevel(Level.ALL);
		
		
		controler.run();

	}
	
	private static Scenario initSampleScenario() {

		Config config = ConfigUtils.loadConfig("C:/Workspace/roadpricingSingapore/scenarios/corridor/localRun/config_corridor.xml");
//		config.controler().setOutputDirectory("C:/Workspace/roadpricingSingapore/output_Corridor/corridor_test");
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
			
			
		//	ScoringFunctionFactoryExtended scoringFunctionFactoryExtended = new ScoringFunctionFactoryExtended(controler.getConfig().planCalcScore(), controler.getNetwork());
		//	PlansScoringExtended plansScoringExtended = new PlansScoringExtended(controler.getScenario(), controler.getEvents(), controler.getControlerIO(), scoringFunctionFactoryExtended);
		//	controler.addControlerListener(plansScoringExtended);
		}
	}

}
