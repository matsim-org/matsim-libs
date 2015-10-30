package playground.artemc.analysis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import playground.artemc.crowding.CrowdednessObserver;
import playground.artemc.crowding.DatabaseSQLwriter;
import playground.artemc.crowding.newScoringFunctions.LoadFactor60ScoringFunctionFactory;
import playground.artemc.crowding.newScoringFunctions.ScoreListener;
import playground.artemc.crowding.newScoringFunctions.ScoreTracker;
import playground.artemc.crowding.rules.SimpleRule;

import java.io.File;
import java.util.HashMap;
/**
 * Created by artemc on 23/9/15.
 */
public class CrowdingToSQLWriter {

	File connectionProperties = new File("/Users/artemc/Workspace/playgrounds/artemc/connections/matsim2postgresLocal.properties");

	private static Integer lastIteration = 1000;
	private static String simType = "5min";
	private static String schema = "corridor_5min_crowd";

	private static String dataPath = "/Volumes/DATA 1 (WD 2 TB)/output_SelectExp1_5p_" + simType + "_1000it_Dwell_TEST/";
	private static String connectionPropertiesPath = "/Users/artemc/Workspace/playgrounds/artemc/connections/matsim2postgresLocal.properties";

	private static CrowdednessObserver observer;
	private static ScoreTracker scoreTracker;
	private static ScoreListener scoreListener;
	private static EventsToScore events2Score;


	public static void main(String[] args) {

		File directory = new File(dataPath);
		File[] fList = directory.listFiles();

		for (File file : fList)

		{
			if (file.isDirectory()) {
				HashMap<String, String> scoreMap = new HashMap<String, String>();

				System.out.println(file.getAbsolutePath());

				Config config = new Config();
				config.addCoreModules();
				ConfigReader configReader = new ConfigReader(config);
				configReader.readFile(file.getAbsolutePath() + "/output_config.xml.gz");
				config.plans().setInputFile(file.getAbsolutePath() + "/output_plans.xml.gz");
				config.plans().setInputPersonAttributeFile(null);
				config.network().setInputFile(file.getAbsolutePath() + "/output_network.xml.gz");
				config.transit().setTransitScheduleFile(file.getAbsolutePath() + "/output_transitSchedule.xml.gz");
				config.transit().setVehiclesFile(file.getAbsolutePath() + "/output_transitVehicles.xml.gz");
				config.plans().setInputPersonAttributeFile(file.getAbsolutePath() + "/output_personAttributes.xml.gz");
				Scenario scenario = ScenarioUtils.loadScenario(config);


				//ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.loadConfig(file.getAbsolutePath() + "/output_config.xml.gz"));
				//new MatsimNetworkReader(scenario).readFile(file.getAbsolutePath() + "/output_network.xml.gz");
				String eventFilePath = file.getAbsolutePath() + "/it." + lastIteration + "/" + lastIteration + ".events.xml.gz";

				EventsManager eventsManager = EventsUtils.createEventsManager(scenario.getConfig());
				//CrowdednessObserver observer = new CrowdednessObserver(scenario, controler.getEvents(), new StochasticRule());
				observer = new CrowdednessObserver(scenario, eventsManager, new SimpleRule());
				scoreTracker = new ScoreTracker();
				scoreListener = new ScoreListener(scoreTracker);

				ScoringFunctionFactory sfFactory = new LoadFactor60ScoringFunctionFactory(new CharyparNagelScoringFunctionFactory(scenario));
				((LoadFactor60ScoringFunctionFactory) sfFactory).setEvents(eventsManager);
				((LoadFactor60ScoringFunctionFactory) sfFactory).setScenario(scenario);
				((LoadFactor60ScoringFunctionFactory) sfFactory).setScoreTracker(scoreTracker);

				events2Score = new EventsToScore(scenario, sfFactory);


				//ScoringFunctionFactory sfFactory = new LoadFactor60ScoringFunctionFactory(new CharyparNagelScoringFunctionFactory(scenario), events2Score, scoreTracker, scenario);


				MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
				eventsManager.addHandler(events2Score);


				eventsManager.addHandler(observer);

				reader.readFile(eventFilePath);

				events2Score.finish();
				scoreListener.finish(1000);
				writeTables(scenario);
			}

		}
	}

	public static void writeTables(Scenario scenario) {

		String population = "8000";
		Integer numberOfIterations = 1000;
		String externalityType = "crowdingM4";

		DatabaseSQLwriter writer = new DatabaseSQLwriter(schema, connectionPropertiesPath);
		try {
			// Write a SQL File describing the bus occupancy and the number of users boarding and alighting at each stations.
			writer.writeSQLCrowdednessObserver("VehicleStates_" + population + "_CrowdingM4_" + externalityType + "_" + numberOfIterations + "It", "ModelM4_" + population + "_" + externalityType, observer.getVehicleStates());

			// Write a SQL File describing score, crowding penalty and externalities of each agent.
			writer.writeSQLPersonScore("PersonScore_" + population + "_CrowdingM4_" + externalityType + "_" + numberOfIterations + "It", "ModelM4_" + population + "_" + externalityType, scoreTracker, scenario);

			// Write a SQL File describing crowding and externalities pro vehicle.
			writer.writeSQLVehicleScore("VehicleScore_" + population + "_CrowdingM4_" + externalityType + "_" + numberOfIterations + "It", "ModelM4_" + population + "_" + externalityType, scoreTracker, scenario);

			// Write a SQL File describing total crowdedness anad total externality pro iteration
			writer.writeSQLScoreProIteration("IterationScore_" + population + "_CrowdingM4_" + externalityType + "_" + numberOfIterations + "It", "ModelM4_" + population + "_" + externalityType, scoreListener);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
