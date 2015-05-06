package playground.artemc.analysis;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationReaderMatsimV5;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import playground.artemc.analysis.postgresql.PostgresType;
import playground.artemc.analysis.postgresql.PostgresqlCSVWriter;
import playground.artemc.analysis.postgresql.PostgresqlColumnDefinition;
import playground.artemc.utils.DataBaseAdmin;

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
 * Created by artemc on 11/3/15.
 */
public class IndividualSelectedBenefitsSQLWriter {

	public static void main(String[] args) {

		File connectionProperties = new File("/Users/artemc/Workspace/playgrounds/artemc/connections/matsim2postgresLocal.properties");
		String schema="corridor";

		String dataPath = "/Volumes/DATA 1 (WD 2 TB)/SimMethanaOutput_280415/";
		String populationFile;


		File directory = new File(dataPath);
		File[] fList = directory.listFiles();

		for (File file : fList) {
			if (file.isDirectory()) {
				HashMap<String, String> scoreMap = new HashMap<String, String>();
				ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

				System.out.println(file.getAbsolutePath());
				populationFile = file.getAbsolutePath() + "/output_plans.xml.gz";

				new PopulationReaderMatsimV5(scenario).readFile(populationFile);
				System.out.println("   Population size: " + scenario.getPopulation().getPersons().size());

				//Read selected plan score
				System.out.println("   Reading population from: " + populationFile);
				for (Person person : scenario.getPopulation().getPersons().values()) {
					scoreMap.put(person.getId().toString(), person.getSelectedPlan().getScore().toString());
				}

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
