package playground.artemc.scenarios;


import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import playground.artemc.analysis.AnalysisControlerListener;
import playground.artemc.annealing.SimpleAnnealer;
import playground.artemc.dwellTimeModel.QSimFactory;
import playground.artemc.scoring.HeterogeneousCharyparNagelScoringFunctionForAnalysisFactory;
import playground.artemc.scoring.DisaggregatedHeterogeneousScoreAnalyzer;
import playground.artemc.socialCost.MeanTravelTimeCalculator;
import playground.artemc.transitRouter.TransitRouterEventsHeteroWSModule;
import playground.artemc.transitRouter.stopStopTimes.StopStopTimeCalculator;
import playground.artemc.transitRouter.waitTimes.WaitTimeStuckCalculator;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;


public class CorridorRun {
	
	
	private static HashMap<Id, Double> factorMap = new HashMap<Id, Double>();
	private static String input;
	private static String output;
	
	public static void main(String[] args){

		input = args[0];
		output = args[1];

		Controler controler = null;
		Scenario scenario = initSampleScenario();
		controler = new Controler(scenario);


		Initializer initializer = new Initializer();
		controler.addControlerListener(initializer);

		ObjectAttributes incomes = new ObjectAttributes();
		ObjectAttributesXmlReader incomesReader = new ObjectAttributesXmlReader(incomes);
		incomesReader.parse(input+"income.xml");


		HashMap<Id, Integer> incomeMap = new HashMap<Id, Integer>();
		
		for(Id personId:scenario.getPopulation().getPersons().keySet()){
			incomeMap.put(personId, (Integer) (incomes.getAttribute(personId.toString(),"income")));
		}

		/*Calculate Income Statistics*/
		Integer sum=0;
		Double mean = 0.0;
		for(Id id:incomeMap.keySet()){
			sum = sum + incomeMap.get(id);
			mean = (double) sum / (double) incomeMap.size();
		}

		/*Create map of personal income factors*/
		for(Id id:incomeMap.keySet()){
			Integer personIncome = incomeMap.get(id);
			factorMap.put(id, Math.log((double) personIncome/mean + 1));
		}

		// Additional analysis
		ScenarioImpl scenarioImpl = (ScenarioImpl) controler.getScenario();
        controler.setScoringFunctionFactory(new HeterogeneousCharyparNagelScoringFunctionForAnalysisFactory(controler.getConfig().planCalcScore(), controler.getScenario().getNetwork()));
		controler.addControlerListener(new SimpleAnnealer());
		
		// Additional analysis
		AnalysisControlerListener analysisControlerListener = new AnalysisControlerListener((ScenarioImpl) controler.getScenario());
		controler.addControlerListener(analysisControlerListener);
		controler.addControlerListener(new DisaggregatedHeterogeneousScoreAnalyzer((ScenarioImpl) controler.getScenario(),analysisControlerListener.getTripAnalysisHandler()));
		controler.run();
		
		controler.setMobsimFactory(new QSimFactory());


		//Routing PT
		WaitTimeStuckCalculator waitTimeCalculator = new WaitTimeStuckCalculator(controler.getScenario().getPopulation(), controler.getScenario().getTransitSchedule(), controler.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (controler.getConfig().qsim().getEndTime()-controler.getConfig().qsim().getStartTime()));
		controler.getEvents().addHandler(waitTimeCalculator);

		StopStopTimeCalculator stopStopTimeCalculator = new StopStopTimeCalculator(controler.getScenario().getTransitSchedule(), controler.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (controler.getConfig().qsim().getEndTime()-controler.getConfig().qsim().getStartTime()));
		controler.getEvents().addHandler(stopStopTimeCalculator);

		controler.addOverridingModule(new TransitRouterEventsHeteroWSModule(waitTimeCalculator.getWaitTimes(), stopStopTimeCalculator.getStopStopTimes()));
		
		
		controler.setOverwriteFiles(true);
		controler.run();


		//Logger root = Logger.getRootLogger();
		//root.setLevel(Level.ALL);
	}

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


			// initialize the social costs disutility calculator
			//TravelTimeAndDistanceBasedIncomeTravelDisutilityFactory factory = new TravelTimeAndDistanceBasedIncomeTravelDisutilityFactory(factorMap);
			//controler.setTravelDisutilityFactory(factory);
			
			//	ScoringFunctionFactoryExtended scoringFunctionFactoryExtended = new ScoringFunctionFactoryExtended(controler.getConfig().planCalcScore(), controler.getNetwork());
			//	PlansScoringExtended plansScoringExtended = new PlansScoringExtended(controler.getScenario(), controler.getEvents(), controler.getControlerIO(), scoringFunctionFactoryExtended);
			//	controler.addControlerListener(plansScoringExtended);
		}
	}

}
