package playground.mmoyo.analysis.comp;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.experimental.ScenarioLoader;
import org.matsim.core.api.experimental.ScenarioLoaderFactoryImpl;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PopulationImpl;

/**Reads many populations and merges them adding a index to repeated persons**/ 
public class PlansMerger {

	/**args are the config files containing the populations to merge*/
	public PlansMerger (String[] configs){
		int populationsNum= configs.length;
		Network net = null;
		Population[] populationArray = new Population[populationsNum];
		Population newPopulation = new PopulationImpl(new ScenarioImpl());
		 
		for (int i=0; i<populationsNum; i++){
			ScenarioLoader sl = new ScenarioLoaderFactoryImpl().createScenarioLoader(configs[i]);
			Scenario scenario = sl.loadScenario();
			populationArray[i] = scenario.getPopulation();
			net = scenario.getNetwork();
		}
		
		for (Person person : populationArray[0].getPersons().values()) {
			Id id = person.getId();
			Person[] personArray= new Person[populationsNum];

			byte noNull = 0;
			for (int i=0; i<populationsNum; i++){
				personArray[i]= populationArray[i].getPersons().get(id);
				if (personArray[i]!=null) noNull++; 
			}
			
			if (noNull== populationsNum ){
				for (byte i=0; i<populationsNum; i++){
					personArray[i].setId(new IdImpl(id.toString() + (i+1)));
					newPopulation.addPerson(personArray[i]);
				}
			}
		}
	
		String outputFile= "../playgrounds/mmoyo/output/mergedPopulation.xml"; 
		System.out.println("writing output plan file..." +  outputFile);
		PopulationWriter popwriter = new PopulationWriter(newPopulation, net);
		popwriter.write(outputFile) ;
		System.out.println("done");
	}

	public static void main(String[] args) {
		String [] configs = new String[3];
		configs[0]="../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/BerlinBrandenburg/routed_1x_subset_xy2links_ptplansonly/config/config_routedPlans.xml";
		configs[1]="../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/BerlinBrandenburg/routed_1x_subset_xy2links_ptplansonly/config/config_routedPlans_MoyoTime.xml";
		configs[2]="../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/BerlinBrandenburg/routed_1x_subset_xy2links_ptplansonly/config/config_routedPlans_MoyoParameterized.xml";
		new PlansMerger(configs);
	}
}


