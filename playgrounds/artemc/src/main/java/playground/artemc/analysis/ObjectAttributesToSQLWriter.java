package playground.artemc.analysis;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationReaderMatsimV5;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
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
 * Created by artemc on 8/5/15.
 */
public class ObjectAttributesToSQLWriter {

	private MutableScenario scenario;

	public static void main(String[] args) {

		String connectionProperties = "/Users/artemc/Workspace/playgrounds/artemc/connections/matsim2postgresLocal.properties";
		String schema = "corridor_5min";
		String table = "incomes8000";

		String populationPath = "/Users/artemc/Dropbox/Work/Input/input17062015_8000_5min/population.xml";
		String personAttributePath = "/Users/artemc/Dropbox/Work/Input/input17062015_8000_5min/personAttributes.xml";

		ObjectAttributesToSQLWriter writer = new ObjectAttributesToSQLWriter();
		writer.readFile(populationPath, personAttributePath);
		writer.writeToDatabase(connectionProperties, schema, table);
	}

	private void readFile(String populationPath, String personAttributePath) {
		this.scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		new PopulationReaderMatsimV5(scenario).readFile(populationPath);
		new ObjectAttributesXmlReader(scenario.getPopulation().getPersonAttributes()).parse(personAttributePath);
	}

	public void writeToDatabase(String connectionPropertiesFile, String schema, String tableName) {
		File connectionProperties = new File(connectionPropertiesFile);
		tableName = schema + "."+tableName;

		HashMap<String, ArrayList<String>> dataMap = new HashMap<String, ArrayList<String>>();

		for (Person person : scenario.getPopulation().getPersons().values()) {
			if (!dataMap.containsKey(person.getId().toString()))
				dataMap.put(person.getId().toString(), new ArrayList<String>());

			String income = scenario.getPopulation().getPersonAttributes().getAttribute(person.getId().toString(),"income").toString();
			String betaFactor = scenario.getPopulation().getPersonAttributes().getAttribute(person.getId().toString(),"betaFactor").toString();
			//String normalFactor = scenario.getPopulation().getPersonAttributes().getAttribute(person.getId().toString(),"normalFactor").toString();


			dataMap.get(person.getId().toString()).add(income);
			dataMap.get(person.getId().toString()).add(betaFactor);
			//dataMap.get(person.getId().toString()).add(normalFactor);
		}

		//Write selected plan scores to Database;
		DateFormat df = new SimpleDateFormat("yyyy_MM_dd HH_mm_ss");
		String formattedDate = df.format(new Date());

		List<PostgresqlColumnDefinition> columns = new ArrayList<>();
		columns.add(new PostgresqlColumnDefinition("person_id", PostgresType.TEXT, "primary key"));
		columns.add(new PostgresqlColumnDefinition("income", PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("betaFactor", PostgresType.FLOAT8));
		//columns.add(new PostgresqlColumnDefinition("normalFactor", PostgresType.FLOAT8));

		try {
			DataBaseAdmin attributesDBA = new DataBaseAdmin(connectionProperties);
			PostgresqlCSVWriter attributesWritter = new PostgresqlCSVWriter("BENEFITS", tableName, attributesDBA, 1000, columns);
			attributesWritter.addComment(String.format("PersonAttributes from output population of %s created on %s.", scenario.getPopulation().getPersons().size(), formattedDate));
			for (String personId : dataMap.keySet()) {
				Object[] data = new Object[3];
				data[0] = personId;
				data[1] = dataMap.get(personId).get(0);
				data[2] = dataMap.get(personId).get(1);
				//data[3] = dataMap.get(personId).get(2);

				attributesWritter.addLine(data);
			}

			attributesWritter.finish();
			attributesDBA.close();
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
