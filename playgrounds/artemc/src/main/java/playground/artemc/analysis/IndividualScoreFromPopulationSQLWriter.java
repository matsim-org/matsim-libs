package playground.artemc.analysis;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import playground.artemc.analysis.postgresql.PostgresType;
import playground.artemc.analysis.postgresql.PostgresqlCSVWriter;
import playground.artemc.analysis.postgresql.PostgresqlColumnDefinition;
import playground.artemc.utils.DataBaseAdmin;
import playground.vsp.analysis.modules.userBenefits.UserBenefitsCalculator;
import playground.vsp.analysis.modules.userBenefits.WelfareMeasure;

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
 * Created by artemc on 22/4/15.
 */
public class IndividualScoreFromPopulationSQLWriter {

	Population population;
	Config config;

	public IndividualScoreFromPopulationSQLWriter(Config config, Population population) {
		this.population = population;
		this.config = config;
	}

	public void writeToDatabase(String connectionPropertiesFile, String schema, String tableName) {
		File connectionProperties = new File(connectionPropertiesFile);
		Integer maximalNumberOfPlans = 0;

		//Read plan scores and calculate user benefits (logsum)
		HashMap<String, ArrayList<String>> dataMap = new HashMap<String, ArrayList<String>>();
		UserBenefitsCalculator userBenefitsCalculator_logsum = new UserBenefitsCalculator(this.config, WelfareMeasure.LOGSUM, false);

		for (Person person : population.getPersons().values()) {
			if (!dataMap.containsKey(person.getId().toString())) dataMap.put(person.getId().toString(), new ArrayList<String>());

			dataMap.get(person.getId().toString()).add(person.getSelectedPlan().getScore().toString());
			if (person.getSelectedPlan().getCustomAttributes().containsKey("toll")) {
				dataMap.get(person.getId().toString()).add((String) person.getSelectedPlan().getCustomAttributes().get("toll"));
			} else {
				dataMap.get(person.getId().toString()).add("0");
			}

			for (Plan plan : person.getPlans()) {
				if (!plan.isSelected()) {
					dataMap.get(person.getId().toString()).add(plan.getScore().toString());
					if (plan.getCustomAttributes().containsKey("toll")) {
						dataMap.get(person.getId().toString()).add((String) plan.getCustomAttributes().get("toll"));
					} else {
						dataMap.get(person.getId().toString()).add("0");
					}

				}
			}

			dataMap.get(person.getId().toString()).add(Double.toString(userBenefitsCalculator_logsum.calculateUtilityOfPerson_utils(person)));

			/*Save maximal number of plans*/
			if (person.getPlans().size() > maximalNumberOfPlans) maximalNumberOfPlans = person.getPlans().size();
		}

		//Write selected plan scores to Database;
		DateFormat df = new SimpleDateFormat("yyyy_MM_dd HH_mm_ss");
		String formattedDate = df.format(new Date());

		List<PostgresqlColumnDefinition> columns = new ArrayList<>();
		columns.add(new PostgresqlColumnDefinition("person_id", PostgresType.TEXT, "primary key"));
		columns.add(new PostgresqlColumnDefinition("selected_score", PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("toll_selected", PostgresType.FLOAT8));
		for (int i = 1; i < maximalNumberOfPlans; i++) {
			columns.add(new PostgresqlColumnDefinition("score_alt" + i, PostgresType.FLOAT8));
			columns.add(new PostgresqlColumnDefinition("toll_alt" + i + "", PostgresType.FLOAT8));
		}

		columns.add(new PostgresqlColumnDefinition("logsum", PostgresType.FLOAT8));

		try {
			DataBaseAdmin individualScoresDBA = new DataBaseAdmin(connectionProperties);
			PostgresqlCSVWriter individualScoresWriter = new PostgresqlCSVWriter("BENEFITS", tableName, individualScoresDBA, 1000, columns);
			individualScoresWriter.addComment(String.format("MATSim selected plan scores from output population of %s created on %s.", config.controler().getOutputDirectory(), formattedDate));
			for (String personId : dataMap.keySet()) {
				Object[] data = new Object[maximalNumberOfPlans*2 + 2];
				data[0] = personId;
				for (int i = 1; i < data.length; i++) {
					data[i] = dataMap.get(personId).get(i - 1);
				}
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
