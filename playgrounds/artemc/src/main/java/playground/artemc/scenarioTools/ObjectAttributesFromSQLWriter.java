package playground.artemc.scenarioTools;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import playground.artemc.plansTools.PlanSelectorByMode;
import playground.artemc.utils.DataBaseAdmin;
import playground.artemc.utils.NoConnectionException;
import playground.artemc.utils.Writer;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

/**
 * Created by artemc on 28/10/15.
 */
public class ObjectAttributesFromSQLWriter {

	private static final Logger log = Logger.getLogger(ObjectAttributesFromSQLWriter.class);

	private static ObjectAttributes incomes = new ObjectAttributes();
	private static HashMap<Id<Person>,Integer> incomeData = new HashMap<Id<Person>,Integer>();

	public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, SQLException, NoConnectionException {
		DataBaseAdmin dba = new DataBaseAdmin(new File("/Users/artemc/Workspace/matsim-git/playgrounds/artemc/connections/sfPostgresLocal.properties"));
		ResultSet persons = dba.executeQuery("SELECT * FROM population.population");

		String inputPopulationFile = args[0];
		String outputAttributesFile = args[1];

		ObjectAttributesFromSQLWriter objectAttributesFromSQLWriter = new ObjectAttributesFromSQLWriter();

		/*Create scenario and load population*/
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		log.info("Reading population...");
		new PopulationReader(scenario).readFile(inputPopulationFile);
		Population population = scenario.getPopulation();


		while(persons.next()){
			String person_id = persons.getString("synth_person_id");
			Integer hhIncome = persons.getInt("HHINCOME");
			incomeData.put(Id.create(person_id, Person.class), hhIncome);
		}

		for(Id<Person> person:population.getPersons().keySet()){
		 Integer income = incomeData.get(person);
		 incomes.putAttribute(person.toString(), "income", income);
			incomes.putAttribute(person.toString(), "betaFactor", 1.0);
		}

		log.info("Writing incomes to " + outputAttributesFile );
		ObjectAttributesXmlWriter attributesWriter = new ObjectAttributesXmlWriter(incomes);
		attributesWriter.writeFile(outputAttributesFile);
	}

}
