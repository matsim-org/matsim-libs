package playground.mmoyo.algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.population.algorithms.PersonAlgorithm;

import playground.mmoyo.io.PopSecReader;


/**list number of agents related to selected plan index in the population*/
public class SelectedPlanLister2 implements PersonAlgorithm {
	private Map <Integer, List<Id>> selIndex2personMap = new TreeMap <Integer, List<Id>>();
	final String STR_SPACE = " ";
	int size=0;
	
	public SelectedPlanLister2(){
	}

	@Override
	public void run(Person person) {
		int i= person.getPlans().indexOf(person.getSelectedPlan());
		if(!selIndex2personMap.keySet().contains(i)){
			selIndex2personMap.put(i, new ArrayList<Id>());
		}
		selIndex2personMap.get(i).add(person.getId());
		size++;
	}
	
	private void printSelectedIndex(){
		System.out.println("Population size: " + size); 
		for(Map.Entry <Integer,List<Id>> entry: selIndex2personMap.entrySet() ){
			Integer key = entry.getKey(); 
			List<Id> indexList = entry.getValue();
			
			System.out.println("\nIndex: " + key + "; selected plans: " + indexList.size() + STR_SPACE);
			for (Id id : entry.getValue()){
				System.out.print(id + STR_SPACE);
			}
		}
	}
	
	public static void main(String[] args) {
		String popFilePath;
		String netFilePath;
		if (args.length>0){
			popFilePath = args[0];
			netFilePath = args[1];
		}else{
			popFilePath = "../../";
			netFilePath = "../../";
		}

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader matsimNetReader = new MatsimNetworkReader(scenario);
		matsimNetReader.readFile(netFilePath);	
		
		SelectedPlanLister2 selectedPlanLister2 = new SelectedPlanLister2();
		new PopSecReader(scenario, selectedPlanLister2).readFile(popFilePath);
		selectedPlanLister2.printSelectedIndex();
	}
	
}