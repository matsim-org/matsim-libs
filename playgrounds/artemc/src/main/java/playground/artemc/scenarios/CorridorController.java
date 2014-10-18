package playground.artemc.scenarios;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import playground.artemc.annealing.SimpleAnnealer;
import playground.artemc.dwellTimeModel.QSimFactory;
import playground.artemc.scoreAnalyzer.DisaggIncomeCharyparNagelScoringFunctionFactory;
import playground.artemc.scoreAnalyzer.DisaggIncomeScoreAnalyzer;
import playground.artemc.scoreAnalyzer.DisaggregatedCharyparNagelScoringFunctionFactory;
import playground.artemc.scoreAnalyzer.DisaggregatedScoreAnalyzer;
import playground.artemc.scoreAnalyzer.TravelTimeAndDistanceBasedIncomeTravelDisutility;
import playground.artemc.scoreAnalyzer.TravelTimeAndDistanceBasedIncomeTravelDisutilityFactory;
import playground.artemc.socialCost.MeanTravelTimeCalculator;
import playground.artemc.socialCost.WelfareAnalysisControlerListener;
import playground.artemc.socialCost.vehicleOccupancy.VehicleOccupancy;
import playground.artemc.transitRouterEventsBased.TransitRouterWSImplFactory;
import playground.artemc.transitRouterEventsBased.TransitRouterWSVImplFactory;
import playground.artemc.transitRouterEventsBased.stopStopTimes.StopStopTimeCalculator;
import playground.artemc.transitRouterEventsBased.vehicleOccupancy.VehicleOccupancyCalculator;
import playground.artemc.transitRouterEventsBased.waitTimes.WaitTimeStuckCalculator;


public class CorridorController {
	
	private static HashMap<Id<Person>, Double> factorMap = new HashMap<Id<Person>, Double>();

	public static void main(String[] args){

		String path = args[0];

		Controler controler = null;
		Scenario scenario = initSampleScenario();
		controler = new Controler(scenario);
		
		Initializer initializer = new Initializer();
		controler.addControlerListener(initializer);

		ObjectAttributes incomes = new ObjectAttributes();
		ObjectAttributesXmlReader incomesReader = new ObjectAttributesXmlReader(incomes);
		incomesReader.parse(path + "/income_1000.xml");


		HashMap<Id<Person>, Integer> incomeMap = new HashMap<Id<Person>, Integer>();
		
		for(Id<Person> personId:scenario.getPopulation().getPersons().keySet()){
			incomeMap.put(personId, (Integer) (incomes.getAttribute(personId.toString(),"income")));
		}

		/*Calculate Income Statistics*/
		Integer sum=0;
		Double mean = 0.0;
		for(Id<Person> id:incomeMap.keySet()){
			sum = sum + incomeMap.get(id);
			mean = (double) sum / (double) incomeMap.size();
		}

		/*Create map of personal income factors*/
		double labmda_inc = -0.1697; 
		for(Id<Person> id:incomeMap.keySet()){
			Integer personIncome = incomeMap.get(id);
			//factorMap.put(id, Math.log((double) personIncome/mean + 1));
			factorMap.put(id, Math.pow((double) personIncome/mean,labmda_inc));
		}

		// Additional analysis
		ScenarioImpl scenarioImpl = (ScenarioImpl) controler.getScenario();
		//controler.setScoringFunctionFactory(new DisaggIncomeCharyparNagelScoringFunctionFactory(controler.getConfig().planCalcScore(), controler.getNetwork(), factorMap));
		controler.setScoringFunctionFactory(new DisaggregatedCharyparNagelScoringFunctionFactory(controler.getConfig().planCalcScore(), controler.getNetwork(), factorMap));
		//controler.addControlerListener(new DisaggIncomeScoreAnalyzer(scenarioImpl));
		controler.addControlerListener(new DisaggregatedScoreAnalyzer(scenarioImpl));
		controler.addControlerListener(new SimpleAnnealer());
		controler.addControlerListener(new WelfareAnalysisControlerListener((ScenarioImpl) controler.getScenario()));
		
		//controler.setMobsimFactory(new QSimFactory());
		
		WaitTimeStuckCalculator waitTimeCalculator = new WaitTimeStuckCalculator(controler.getPopulation(), controler.getScenario().getTransitSchedule(), controler.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (controler.getConfig().qsim().getEndTime()-controler.getConfig().qsim().getStartTime()));
		controler.getEvents().addHandler(waitTimeCalculator);
		StopStopTimeCalculator stopStopTimeCalculator = new StopStopTimeCalculator(controler.getScenario().getTransitSchedule(), controler.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (controler.getConfig().qsim().getEndTime()-controler.getConfig().qsim().getStartTime()));
		controler.getEvents().addHandler(stopStopTimeCalculator);
		//VehicleOccupancyCalculator vehicleOccupancyCalculator = new VehicleOccupancyCalculator(controler.getScenario().getTransitSchedule(), controler.getScenario().getVehicles(), controler.getConfig());
		//controler.getEvents().addHandler(vehicleOccupancyCalculator);
		//controler.setTransitRouterFactory(new TransitRouterWSVImplFactory(controler.getScenario(), waitTimeCalculator.getWaitTimes(), stopStopTimeCalculator.getStopStopTimes(), vehicleOccupancyCalculator.getVehicleOccupancy()));
		controler.setTransitRouterFactory(new TransitRouterWSImplFactory(controler.getScenario(), waitTimeCalculator.getWaitTimes(), stopStopTimeCalculator.getStopStopTimes()));
		
		controler.setOverwriteFiles(true);
		controler.run();

		//Logger root = Logger.getRootLogger();
		//root.setLevel(Level.ALL);
	}

	private static Scenario initSampleScenario() {

		Config config = ConfigUtils.loadConfig("C:/Work/localRun/config_corridor.xml");
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

			// initialize the social costs disutility calculator
			TravelTimeAndDistanceBasedIncomeTravelDisutilityFactory factory = new TravelTimeAndDistanceBasedIncomeTravelDisutilityFactory(factorMap);
			controler.setTravelDisutilityFactory(factory);
			
			//	ScoringFunctionFactoryExtended scoringFunctionFactoryExtended = new ScoringFunctionFactoryExtended(controler.getConfig().planCalcScore(), controler.getNetwork());
			//	PlansScoringExtended plansScoringExtended = new PlansScoringExtended(controler.getScenario(), controler.getEvents(), controler.getControlerIO(), scoringFunctionFactoryExtended);
			//	controler.addControlerListener(plansScoringExtended);
		}
	}

}
