package playground.santiago.utils;

import java.util.ArrayList;
import java.util.List;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

public class CleaningPlans {

	static String originalPlansFile = "../../../runs-svn/santiago/baseCase1pct/inputForStepSP1_0/step.0_100.plans.xml.gz";
	
	//for step1-e1 and step1-e2, stepA1, stepP1_1, stepSP1_0
	
	static String selectedPlansFile = "../../../runs-svn/santiago/baseCase1pct/inputForStepSP1_0/selected_step.0_100.plans.xml.gz";
	
	public static void main(String[] args) {
		
		Scenario originalScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());		
		PopulationReader pr = new PopulationReader(originalScenario);
		pr.readFile(originalPlansFile);
		Population originalPopulation = originalScenario.getPopulation();
		
		//Creating cleaned plans//
		Scenario selectedScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population selectedPopulation = selectedScenario.getPopulation();
		
		List<Person> persons = new ArrayList<>(originalPopulation.getPersons().values());
		
		for (Person p : persons) {
			PersonUtils.removeUnselectedPlans(p);
			selectedPopulation.addPerson(p);
			}
		
		new PopulationWriter(selectedPopulation).write(selectedPlansFile);
		
		
		
		}
}
