package playground.artemc.analysis;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.PopulationReaderMatsimV5;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import playground.artemc.analysis.postgresql.PostgresType;
import playground.artemc.analysis.postgresql.PostgresqlCSVWriter;
import playground.artemc.analysis.postgresql.PostgresqlColumnDefinition;
import playground.artemc.crowding.CrowdednessObserver;
import playground.artemc.crowding.DatabaseSQLwriter;
import playground.artemc.crowding.newScoringFunctions.LoadFactor60ScoringFunctionFactory;
import playground.artemc.crowding.newScoringFunctions.ScoreListener;
import playground.artemc.crowding.newScoringFunctions.ScoreTracker;
import playground.artemc.crowding.rules.SimpleRule;
import playground.artemc.utils.DataBaseAdmin;
import playground.artemc.utils.NoConnectionException;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by artemc on 23/9/15.
 */
public class CrowdingToSQLWriter {

	File connectionProperties = new File("/Users/artemc/Workspace/playgrounds/artemc/connections/matsim2postgresLocal.properties");

	private static Integer lastIteration = 1000;

	private static String simType = "5min";
	private static String schema = "corridor_5min";

	private static String dataPath = "/Volumes/DATA 1 (WD 2 TB)/output_SelectExp1_5p_" + simType + "_1000it_Dwell/";
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

				ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.loadConfig(file.getAbsolutePath() + "/output_config.xml.gz"));
				new MatsimNetworkReader(scenario).readFile(file.getAbsolutePath() + "/output_network.xml.gz");

				String eventFilePath = file.getAbsolutePath() + "/it." + lastIteration + "/" + lastIteration + ".events.xml.gz";
				EventsManager eventsManager = EventsUtils.createEventsManager();

				//CrowdednessObserver observer = new CrowdednessObserver(scenario, controler.getEvents(), new StochasticRule());
				observer = new CrowdednessObserver(scenario, eventsManager, new SimpleRule());

				scoreTracker = new ScoreTracker();
				scoreListener = new ScoreListener(scoreTracker);

				eventsManager.addHandler(observer);
				new MatsimEventsReader(eventsManager).readFile(eventFilePath);

				ScoringFunctionFactory sfFactory = new LoadFactor60ScoringFunctionFactory(new CharyparNagelScoringFunctionFactory(scenario));
				events2Score = new EventsToScore(scenario, sfFactory);
				events2Score.finish();
				writeTables(scenario);
			}

		}
	}

	public static void writeTables(ScenarioImpl scenario) {

		String population = "8000";
		Integer numberOfIterations = 1000;
		String externalityType = "crowdingM4";

		DatabaseSQLwriter writer = new DatabaseSQLwriter("u_guillaumer", "postgres.properties");
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


/**

			//Write selected plan scores to Database;
			String individualSelectedBenefitsTableName = schema+".scores";
			String tableSuffix = file.getAbsolutePath().split("280415/w8-18")[1];
			tableSuffix = tableSuffix.replaceAll("\\.0x", "x");
			tableSuffix = tableSuffix.replaceAll("\\.5", "5");
			individualSelectedBenefitsTableName = individualSelectedBenefitsTableName + tableSuffix;

			DateFormat df = new SimpleDateFormat("yyyy_MM_dd");
			String formattedDate = df.format(new Date());

			List<PostgresqlColumnDefinition> columns = new ArrayList<>();
			columns.add(new PostgresqlColumnDefinition("person_id",
			                                           PostgresType.TEXT, "primary key"));
			columns.add(new PostgresqlColumnDefinition("selected_score",
			                                           PostgresType.FLOAT8));
			try {
				DataBaseAdmin individualScoresDBA = new DataBaseAdmin(connectionProperties);
				PostgresqlCSVWriter individualScoresWriter = new PostgresqlCSVWriter("BENEFITS", individualSelectedBenefitsTableName, individualScoresDBA, 1000, columns);
				individualScoresWriter.addComment(String.format("MATSim selected plan scores from output plans file %s, created on %s.",
				                                                populationFile, formattedDate));
				for (String personId : scoreMap.keySet()) {
					Object[] data = {personId, scoreMap.get(personId)};
					individualScoresWriter.addLine(data);
				}

				individualScoresWriter.finish();
				individualScoresDBA.close();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (SQLException e) {
				e.printStackTrace();
			}

		}
	}

	System.out.println("Done!");

}

}
**/