package playground.artemc.crowding.run;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;

import playground.artemc.crowding.CrowdednessObserver;
import playground.artemc.crowding.DatabaseSQLwriter;
import playground.artemc.crowding.internalization.InternalizationPtControlerListener;
import playground.artemc.crowding.rules.SimpleRule;
import playground.artemc.socialCost.MeanTravelTimeCalculator;
import playground.artemc.crowding.newScoringFunctions.LoadFactor60ScoringFunctionFactory;
import playground.artemc.crowding.newScoringFunctions.ScoreListener;
import playground.artemc.crowding.newScoringFunctions.ScoreTracker;


/**
 * This Controler run a simulation whose crowdedness effects and externalities 
 * are taken in account, according to the research of Tirachini (2013) and Kaddoura (2013). 
 * The occupancy in buses and the score of each agent are written in two differents SQL Tables. 
 * The dwell time at facilities is calculated using the Sun's Model (2013). 
 * Supplementary, plots of the mean travel time per mode are created.
 * 
 * @author achakirov
 * @author grerat
 * @author pbouman
 * 
 */

public class CrowdingM4CorridorControler {

	private final static Logger log = Logger.getLogger(CrowdingM4CorridorControler.class);
	private static CrowdednessObserver observer;
	private static Controler controler;
	private static ScoreTracker scoreTracker;
	private static ScoreListener scoreListener;
	private static Scenario scenario;
	private static int numberOfIterations;

	public static void main(String[] args){

		scenario = initSampleScenario();
		if (args.length == 0) {
			controler = new Controler(scenario);
		} else controler = new Controler(args);

		//CrowdednessObserver observer = new CrowdednessObserver(scenario, controler.getEvents(), new StochasticRule());
		observer = new CrowdednessObserver(scenario, controler.getEvents(), new SimpleRule());
		controler.getEvents().addHandler(observer);
		scoreTracker = new ScoreTracker();
		scoreListener = new ScoreListener(scoreTracker);

		// Set the Scoring Function LoadFactor60ScoringFunction (model M4)
		controler.setScoringFunctionFactory(new LoadFactor60ScoringFunctionFactory(
				new CharyparNagelScoringFunctionFactory(scenario),
				controler.getEvents(), scoreTracker, controler));

		// Set the Second-Best pricing function
//		controler.setScoringFunctionFactory(new SecondBestScoringFunctionFactory(
//				new CharyparNagelScoringFunctionFactory(scenario.getConfig().planCalcScore(), scenario.getNetwork()),
//				controler.getEvents(), scoreTracker, controler));
		
		Initializer initializer = new Initializer();
		IterationEndsHandler iterationEndsHandler = new IterationEndsHandler();
		ShutdownHandler shutdownHandler = new ShutdownHandler();
		controler.addControlerListener(initializer);
		controler.addControlerListener(iterationEndsHandler);
		controler.addControlerListener(shutdownHandler);
		controler.addControlerListener(scoreListener);
		
		// Kaddoura's externalities
		controler.addControlerListener(new InternalizationPtControlerListener( (ScenarioImpl) controler.getScenario(), scoreTracker));
		
		// L. Sun's Dwell Time
		//controler.setMobsimFactory(new QSimFactory());
		
		controler.run();
	}

	private static Scenario initSampleScenario() {

		numberOfIterations = 1;
		
		Config config = ConfigUtils.loadConfig("./corridor_input/config_CrowdingM4_shortDay_walk.xml");
		config.controler().setOutputDirectory("./TEST2");
		config.controler().setLastIteration(numberOfIterations);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		return scenario;
	}

	private static class IterationEndsHandler implements IterationEndsListener {

		// To avoid an addition of the externalities along the iterations
		public void notifyIterationEnds(IterationEndsEvent event) {
			if(event.getIteration()<numberOfIterations){
			scoreTracker.getPersonScores().clear();
			scoreTracker.getVehicleExternalities().clear();
			scoreTracker.setTotalCrowdednessUtility(0.0);
			scoreTracker.setTotalCrowdednessExternalityCharges(0.0);
			scoreTracker.setTotalInVehicleTimeDelayExternalityCharges(0.0);
			scoreTracker.setTotalCapacityConstraintsExternalityCharges(0.0);
			scoreTracker.setTotalMoneyPaid(0.0);
			}
		}
		
	}


	private static class ShutdownHandler implements ShutdownListener{

		String population = "WithCar";
		String externalityType = "NoExternalities";
		
		public void notifyShutdown(ShutdownEvent event) {
			DatabaseSQLwriter writer = new DatabaseSQLwriter("u_guillaumer", "postgres.properties");
			try {
				// Write a SQL File describing the bus occupancy and the number of users boarding and alighting at each stations.
				writer.writeSQLCrowdednessObserver("VehicleStates_"+population+"_CrowdingM4_"+externalityType +"_"+numberOfIterations+"It", "ModelM4_"+population+"_"+externalityType, observer.getVehicleStates());
				
				// Write a SQL File describing score, crowding penalty and externalities of each agent. 
				writer.writeSQLPersonScore("PersonScore_"+population+"_CrowdingM4_"+externalityType +"_"+numberOfIterations+"It", "ModelM4_"+population+"_"+externalityType, scoreTracker, scenario);
			
				// Write a SQL File describing crowding and externalities pro vehicle.
				writer.writeSQLVehicleScore("VehicleScore_"+population+"_CrowdingM4_"+externalityType +"_"+numberOfIterations+"It", "ModelM4_"+population+"_"+externalityType, scoreTracker, scenario);
				
				// Write a SQL File describing total crowdedness anad total externality pro iteration
				writer.writeSQLScoreProIteration("IterationScore_"+population+"_CrowdingM4_"+externalityType +"_"+numberOfIterations+"It", "ModelM4_"+population+"_"+externalityType, scoreListener);
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
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

