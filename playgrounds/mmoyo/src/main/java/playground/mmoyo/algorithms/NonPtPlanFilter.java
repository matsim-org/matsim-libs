package playground.mmoyo.algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;

import playground.mmoyo.utils.DataLoader;

/**filters out persons without pt legs*/
public class NonPtPlanFilter {
	PtLegFilter ptLegFilter = new PtLegFilter();
	
	public void run (Population pop){
		List<Id> list = new ArrayList<Id>();	
		for (Person person : pop.getPersons().values()){
			if (!ptLegFilter.judge(person)){
				list.add(person.getId());
			}
		}
		
		for(Id id :list){
			pop.getPersons().remove(id);
		}
		//pop.getPersons().keySet().retainAll(list);
	}

	public static void main(String[] args) {
		String popFile;
		String netFile;

		if (args.length>0){
			popFile = args[0];
			netFile = args[1];
		}else{
			popFile = "../../";
			netFile = "../../";
		}
		
		DataLoader dataLoader = new DataLoader();
		Scenario scn = dataLoader.readNetwork_Population(netFile, popFile);
		
		//NonPtPlanFilter nonPtPlanFilter = new NonPtPlanFilter();
		Population pop = scn.getPopulation();
		new NonPtPlanFilter().run(pop);
		
		//write file
		System.out.println("writing output plan file...");
		PopulationWriter popwriter = new PopulationWriter(pop, scn.getNetwork());
		File file = new File(popFile);
		popwriter.write(file.getParent() + "/" + file.getName() + "plansWtPtLegs.xml.gz") ;
		System.out.println("done");
	}
}
