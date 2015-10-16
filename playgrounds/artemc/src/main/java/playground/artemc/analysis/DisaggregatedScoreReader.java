package playground.artemc.analysis;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.Controler;
import playground.artemc.utils.CSVReader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by artemc on 10/6/15.
 */
public class DisaggregatedScoreReader{

	String connectionPropertiesPath = "/Users/artemc/Workspace/playgrounds/artemc/connections/matsim2postgresLocal.properties";
	String schema = "corridor";

	private static final Logger log = Logger.getLogger(DisaggregatedScoreReader.class);

	HashMap<String, ArrayList<String>> scores;

	private Controler controler;
	private Population population;
	private HashMap<Id<Person>, Plan> initialPlans = new HashMap<Id<Person>, Plan>();
	static Integer departureTimeChoices = 3;

	public DisaggregatedScoreReader() {
		this.scores = new HashMap<String, ArrayList<String>>();
	}

	public static void main(String[] args) throws IOException {

		String dataFolder = args[0];
		File directory = new File(dataFolder);
		File[] fList = directory.listFiles();
		DisaggregatedScoreReader scoreReader = new DisaggregatedScoreReader();

		for (File file : fList) {
			if (file.isDirectory()) {
				String fileName = file.getAbsolutePath()+"/"+"timeCostPerPerson.it1200.csv";
				File dataFile = new File(fileName);
				if(dataFile.exists()) {
					scoreReader.readScoresFile(fileName);
					Integer relativeOutputDirectory = file.getAbsolutePath().split("/").length;
					String tableName = scoreReader.schema + ".cost_";
					String tableSuffix = file.getAbsolutePath().split("/")[relativeOutputDirectory - 1];
					tableSuffix = tableSuffix.replaceAll("\\.0x", "x");
					tableSuffix = tableSuffix.replaceAll("-", "_");
					tableSuffix = tableSuffix.replaceAll("\\.5", "5");
					tableSuffix = tableSuffix.replaceAll("\\.1", "1");
					tableName = tableName + tableSuffix;

					IndividualCostToSQLWriter sqlWriter = new IndividualCostToSQLWriter(scoreReader.scores);
					sqlWriter.writeToDatabase(scoreReader.connectionPropertiesPath, scoreReader.schema, tableName);
				}
			}
		}
	}

	public void readScoresFile(String file) throws IOException {
		ArrayList<String[]> personScores = CSVReader. readCSVskip1stLine(file, ";");
		for(String[] line:personScores){
			this.scores.put(line[0],new ArrayList<String>());
			for(int i=1;i<line.length;i++){
				this.scores.get(line[0]).add(line[i]);
			}
		}
	}
}
