package playground.mmoyo.analysis.comp;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.ScenarioLoader;
import org.matsim.core.api.experimental.ScenarioLoaderFactoryImpl;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationWriter;

import java.util.ArrayList;
import java.util.List;

/**Creates a plan with an agent repeated many times*/
public class PlanRepeater {

	
	public PlanRepeater(final String config, final int repetitions, final Id selectedId) {
		
		final String SEPARATOR = "_";
		
		//load scenario
		ScenarioLoader sl = new ScenarioLoaderFactoryImpl().createScenarioLoader(config);
		ScenarioImpl scenarioImpl = (ScenarioImpl) sl.loadScenario();
		Population population = scenarioImpl.getPopulation();
		
		//create selected person
		Person person = new PersonImpl(selectedId);
		person = population.getPersons().get(selectedId);
		
		for (PlanElement pe : person.getSelectedPlan().getPlanElements()){
			if (pe instanceof Leg) {
				Leg leg = (Leg)pe;
				if (leg.getMode().equals(TransportMode.walk))leg.setMode(TransportMode.undefined);
			}
		}
		
		//erase all persons
		List<Id> idList = new ArrayList<Id>();
		for (Id id: population.getPersons().keySet())	idList.add(id);
		for (Id id: idList)       population.getPersons().remove(id);
		
		//add the repeated plan x times
		for (int i=0 ; i<repetitions ; i++) {
			Id newId = new IdImpl(selectedId.toString() + SEPARATOR + i);
			Person personClon = new PersonImpl(newId);
			personClon.addPlan(person.getSelectedPlan());
			//personClon.setId(newId);
			population.addPerson(personClon);
		}
		
		//write this strange plan in output
		System.out.println("writing output plan file...");
		new PopulationWriter(population, scenarioImpl.getNetwork()).writeFile("../playgrounds/mmoyo/output/repeatedPlans" + repetitions + ".xml");
		System.out.println("Done");
	
	}
	
	public static void main(String[] args) {
		String config = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/BerlinBrandenburg/routed_1x_subset_xy2links_ptplansonly/fragmented/config/config_routedPlans.xml";
		int repetitions= 200;
		Id selectedId= new IdImpl("11100153_a");
		new PlanRepeater(config, repetitions, selectedId);
	}
	
}
