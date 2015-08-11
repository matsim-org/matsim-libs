package playground.dhosse.cl.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

public class PopulationAnalysis {

	private static final String runPath = "../../runs-svn/santiago/run7/output/";
	
	private static final String analysisPath = "../../shared-svn/studies/countries/cl/analysis/";
	
	public static void main(String[] args) {

		getPersonsWithNegativeScores();
		
	}
	
	private static void getPersonsWithNegativeScores(){
		
		String plansFile = runPath + ".output_plans.xml.gz";
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(scenario).parse(plansFile);
		
		Set<Person> plansWithNegativeScores = new HashSet<Person>();
		
		for(Person person : scenario.getPopulation().getPersons().values()){
			
			if(person.getSelectedPlan().getScore() < 0){
				
				plansWithNegativeScores.add(person);
				
			}
			
		}
		BufferedWriter writer = IOUtils.getBufferedWriter(analysisPath + "negativeScores.txt");
		
		try {
			
			for(Person person : plansWithNegativeScores){
				
				writer.write(person.getId().toString() + "\t" + person.getSelectedPlan().getScore());
				
				writer.newLine();
				
			}
			
			writer.flush();
			writer.close();
			
		} catch (IOException e) {
			
			e.printStackTrace();
			
		}
		
	}

}