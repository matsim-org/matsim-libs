package playground.artemc.analysis;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationReaderMatsimV5;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import playground.artemc.utils.Writer;

import java.io.File;
import java.util.HashMap;

/**
 * Created by artemc on 11/3/15.
 */
public class IndividualSelectedBenefitsCSVWriter {

	public static void main(String[] args) {

		String dataPath = "/Volumes/DATA 1 (WD 2 TB)/SimMethanaOutput_120215/";
		String populationFile = "/Volumes/DATA 1 (WD 2 TB)/SimMethanaOutput_120215/w8-18_homo/output_plans.xml.gz";
		String individualSelectedScoreFile = "/Volumes/DATA 1 (WD 2 TB)/SimMethanaOutput_120215/w8-18_homo/individualSelectedBenefits";


		File directory = new File(dataPath);
		File[] fList = directory.listFiles();

		for (File file : fList) {
			if (file.isDirectory()) {
				HashMap<String, String> scoreMap = new HashMap<String, String>();
				ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

				System.out.println(file.getAbsolutePath());
				populationFile = file.getAbsolutePath() + "/output_plans.xml.gz";
				individualSelectedScoreFile = file.getAbsolutePath() + "/individualSelectedBenefits.csv";



				new PopulationReaderMatsimV5(scenario).readFile(populationFile);
				System.out.println("   Population size: " + scenario.getPopulation().getPersons().size());

				//Read selected plan score
				System.out.println("   Reading population from: " + populationFile);
				for (Person person : scenario.getPopulation().getPersons().values()) {
					scoreMap.put(person.getId().toString(), person.getSelectedPlan().getScore().toString());
				}

				System.out.println("   Map size: " + scoreMap.size());
				//Write selected plan scores;
				System.out.println("   Writting individial selected plan scores to: " + individualSelectedScoreFile);
				Writer writer = new Writer();
				writer.creteFile(individualSelectedScoreFile);

				for (String personId : scoreMap.keySet()) {
					writer.writeLine(personId + "," + scoreMap.get(personId));
				}

				writer.close();

			}
		}

		System.out.println("Done!");

	}

}
