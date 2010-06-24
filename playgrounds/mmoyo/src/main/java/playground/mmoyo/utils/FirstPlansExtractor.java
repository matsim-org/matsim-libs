package playground.mmoyo.utils;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.api.core.v01.Id;

/**Reads a plan file, selects the first 20 plans and creates a new plan file with them*/ 
public class FirstPlansExtractor {
	int agentNum=20;
	
	public Population run(Population pop){
		ScenarioImpl tempScenario =new ScenarioImpl();
		PopulationImpl outputPop = new PopulationImpl(tempScenario);
		
		int i=0;
		Iterator<?> iter = pop.getPersons().entrySet().iterator();
		while (iter.hasNext() && i<agentNum) {
			Map.Entry<Id, Person> entry = (Entry<Id, Person>) iter.next();
			Person person = entry.getValue();
			outputPop.addPerson(person);
			System.out.println(i + " " +  person.getId());
			i++;
		}
		iter = null;
		return outputPop;
	}

	public static void main(String[] args) {
		String configFile;
		
		if (args.length==1){
			configFile= args[0];
		}else{
			configFile= "../playgrounds/mmoyo/output/Cadyts/output/350_config.xml";
		}
		
		ScenarioLoaderImpl scenarioLoader = new ScenarioLoaderImpl(configFile);
		ScenarioImpl scenario = scenarioLoader.getScenario();
		scenarioLoader.loadScenario();
		
		System.out.println(scenario.getConfig().plans().getInputFile());
		System.out.println("writing output plan file...");
		PopulationWriter popwriter = new PopulationWriter(new FirstPlansExtractor().run(scenario.getPopulation()),scenario.getNetwork());
		popwriter.write(scenario.getConfig().controler().getOutputDirectory() + "it350_20plans.xml") ;
		System.out.println("done");
	}
}