package playground.mmoyo.algorithms;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;

/**Creates a population with only one agent repeated many times*/
public class PersonRepeater {

	
	public PersonRepeater(final String config, final int repetitions, final Id<Person> selectedId) {
		
		final String SEPARATOR = "_";
		
		//load scenario
		ScenarioLoaderImpl sl = ScenarioLoaderImpl.createScenarioLoaderImplAndResetRandomSeed(config);
		ScenarioImpl scenarioImpl = (ScenarioImpl) sl.loadScenario();
		Population population = scenarioImpl.getPopulation();
		
		//create selected person
		Person person = new PersonImpl(selectedId);
		person = population.getPersons().get(selectedId);
		
		//erase all persons
		List<Id<Person>> idList = new ArrayList<>();
		for (Id<Person> id: population.getPersons().keySet())	idList.add(id);
		for (Id<Person> id: idList)       population.getPersons().remove(id);
		
		//add the repeated plan x times
		for (int i=0 ; i<repetitions ; i++) {
			Id<Person> newId = Id.create(selectedId.toString() + SEPARATOR + i, Person.class);
			Person personClon = new PersonImpl(newId);
			
			for (Plan plan: person.getPlans()){
				personClon.addPlan(plan);
			}
			population.addPerson(personClon);
		}
		
		//write this strange population in output
		System.out.println("writing output plan file...");
		new PopulationWriter(population, scenarioImpl.getNetwork()).write(scenarioImpl.getConfig().controler().getOutputDirectory() + repetitions + ".xml");
		System.out.println("Done");
	
	}
	
	public static void main(String[] args) {
		String config = "";
		int repetitions= 10;
		Id<Person> selectedId= Id.create("1", Person.class);
		new PersonRepeater(config, repetitions, selectedId);
	}
	
}
