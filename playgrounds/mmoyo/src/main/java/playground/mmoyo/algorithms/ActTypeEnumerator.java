package playground.mmoyo.algorithms;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.population.algorithms.PersonAlgorithm;

import playground.mmoyo.io.PopSecReader;
import playground.mmoyo.utils.DataLoader;

public class ActTypeEnumerator implements PersonAlgorithm{
	List<String> typeList = new ArrayList<String>();
	
	@Override
	public void run(Person person) {
		for (Plan plan : person.getPlans()){
			for (PlanElement pe : plan.getPlanElements()){
				if (pe instanceof Activity) {
					Activity act = (Activity)pe;
					if(!typeList.contains(act.getType())){
						typeList.add(act.getType());
					}
				}
			}
		}
	}
	
	public void ListTypes (){
		for (String type : typeList){
			System.out.println(type);
		}
	}
	
	public static void main(String[] args) {
		String populationFile = "../../input/juni/newDemand/bvg.run190.25pct.100Cleaned.plans.xml.gz";
		String networkFile = "../../input/juni/newDemand/multimodalNet.xml.gz";
		
		DataLoader dataLoader = new DataLoader();
		ScenarioImpl scn = (ScenarioImpl) dataLoader.createScenario();
		MatsimNetworkReader matsimNetReader = new MatsimNetworkReader(scn);
		matsimNetReader.readFile(networkFile);
		
		ActTypeEnumerator actTypeEnumerator = new ActTypeEnumerator(); 
		PopSecReader popSecReader = new PopSecReader (scn, actTypeEnumerator);
		popSecReader.readFile(populationFile);
		
		actTypeEnumerator.ListTypes();

	}



}
