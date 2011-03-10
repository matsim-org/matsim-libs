package playground.mmoyo.utils;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;

/**Creates a plan with an agent repeated many times*/
public class PlanRepeater {

	
	public PlanRepeater(final String config, final int repetitions, final Id selectedId) {
		
		final String SEPARATOR = "_";
		
		//load scenario
		ScenarioLoaderImpl sl = ScenarioLoaderImpl.createScenarioLoaderImplAndResetRandomSeed(config);
		ScenarioImpl scenarioImpl = (ScenarioImpl) sl.loadScenario();
		Population population = scenarioImpl.getPopulation();
		
		//create selected person
		Person person = new PersonImpl(selectedId);
		person = population.getPersons().get(selectedId);
		
		//erase all persons
		List<Id> idList = new ArrayList<Id>();
		for (Id id: population.getPersons().keySet())	idList.add(id);
		for (Id id: idList)       population.getPersons().remove(id);
		
		//add the repeated plan x times
		for (int i=0 ; i<repetitions ; i++) {
			Id newId = new IdImpl(selectedId.toString() + SEPARATOR + i);
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
		String config = "../playgrounds/mmoyo/test/input/playground/mmoyo/CadytsIntegrationTest/testCalibration/equil_config.xml";
		int repetitions= 10;
		Id selectedId= new IdImpl("1");
		new PlanRepeater(config, repetitions, selectedId);
	}
	
}
