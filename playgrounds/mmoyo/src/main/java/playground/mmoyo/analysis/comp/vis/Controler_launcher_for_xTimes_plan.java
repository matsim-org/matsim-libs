package playground.mmoyo.analysis.comp.vis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
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

import playground.mzilske.bvg09.TransitControler;

public class Controler_launcher_for_xTimes_plan {
	
	public static void main(String[] args) {
		int repetitions= 200;
		String SEPARATOR = "_";
		
		//load scenario
		String config = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/lines344_M44/counts/chen/configs/configWithPtCounts_onlyPT_rieser.xml";
		
		config = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/BerlinBrandenburg/routed_1x_subset_xy2links_ptplansonly/no_fragmented/config/config_routedPlans.xml";
		
		ScenarioLoader sl = new ScenarioLoaderFactoryImpl().createScenarioLoader(config);
		ScenarioImpl scenarioImpl = (ScenarioImpl) sl.loadScenario();
		Population population = scenarioImpl.getPopulation();
		
		/*
		//create selected person
		Id selectedId= new IdImpl("11100153_a");
		Person person = new PersonImpl(selectedId);
		person = population.getPersons().get(selectedId);
		
		for (PlanElement pe : person.getSelectedPlan().getPlanElements()){
			if (pe instanceof Leg) {
				Leg leg = (Leg)pe;
				if (leg.getMode().equals(TransportMode.undefined))leg.setMode(TransportMode.walk);
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
		new PopulationWriter(population, scenarioImpl.getNetwork()).writeFile("../playgrounds/mmoyo/output/repeatedPlans.xml");
		System.out.println("Done");
		*/
		
		TransitControler controler = new TransitControler (scenarioImpl );
		controler.setOverwriteFiles(true);
		controler.run();
	
	}
}
