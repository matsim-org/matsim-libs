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

	public IndividualScoreFromPopulationSQLWriter(Config config, Population population){
        this.population = population;
		this.config = config;
	}

	public void writeToDatabase(String connectionPropertiesFile, String schema, String tableName){
		File connectionProperties = new File(connectionPropertiesFile);
		Integer maximalNumberOfPlans = 0;

		//Read plan scores and calculate user benefits (logsum)
		HashMap<String, ArrayList<String>> scoreMap = new HashMap<String, ArrayList<String>>();
		UserBenefitsCalculator userBenefitsCalculator_logsum = new UserBenefitsCalculator(this.config, WelfareMeasure.LOGSUM, false);

		for (Person person : population.getPersons().values()) {
			if(!scoreMap.containsKey(person.getId().toString()))
				scoreMap.put(person.getId().toString(), new ArrayList<String>());
			scoreMap.get(person.getId().toString()).add(person.getSelectedPlan().getScore().toString());

			for(Plan plan:person.getPlans()){
				if(!plan.isSelected()){
					scoreMap.get(person.getId().toString()).add(plan.getScore().toString());
				}
			}

			scoreMap.get(person.getId().toString()).add(Double.toString(userBenefitsCalculator_logsum.calculateUtilityOfPerson_utils(person)));

			/*Save maximal number of plans*/
			if(person.getPlans().size()>maximalNumberOfPlans)
				maximalNumberOfPlans = person.getPlans().size();
		}

		//Write selected plan scores to Database;
		DateFormat df = new SimpleDateFormat("yyyy_MM_dd");
		String formattedDate = df.format(new Date());

		List<PostgresqlColumnDefinition> columns = new ArrayList<>();
		columns.add(new PostgresqlColumnDefinition("person_id",
		                                           PostgresType.TEXT, "primary key"));
		columns.add(new PostgresqlColumnDefinition("selected_score",
		                                           PostgresType.FLOAT8));
		for(int i=1;i<maximalNumberOfPlans;i++) {
			columns.add(new PostgresqlColumnDefinition("score_alt"+i, PostgresType.FLOAT8));
		}

		columns.add(new PostgresqlColumnDefinition("logsum",
		                                           PostgresType.FLOAT8));

		try {
			DataBaseAdmin individualScoresDBA = new DataBaseAdmin(connectionProperties);
			PostgresqlCSVWriter individualScoresWriter = new PostgresqlCSVWriter("BENEFITS", tableName, individualScoresDBA, 1000, columns);
			individualScoresWriter.addComment(String.format("MATSim selected plan scores from output population of %s created on %s.",
			                                                config.controler().getOutputDirectory(), formattedDate));
			for (String personId : scoreMap.keySet()) {
				Object[] data = new Object[maximalNumberOfPlans+2];
				data[0] = personId;
				for(int i=1;i<data.length;i++){
					data[i] = scoreMap.get(personId).get(i-1);
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
