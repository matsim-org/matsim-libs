package playground.mmoyo.algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.population.algorithms.PersonAlgorithm;

import playground.mmoyo.io.PopSecReader;
import playground.mmoyo.utils.DataLoader;

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
		if (args.length>0){
			popFilePath = args[0];
		}else{
			popFilePath = "../mmoyo/output/taste/seeds/3/10.plans.xml.gz";
			//popFilePath = "../mmoyo/output/taste/inputPlans/500.plans_fromCalibM44.xml.gzprobDistribSelectPlan.xml.gz";
		}

		DataLoader dataLoader = new DataLoader ();
		ScenarioImpl scn = (ScenarioImpl) dataLoader.createScenario(); 
			
		SelectedPlanLister2 selectedPlanLister2 = new SelectedPlanLister2();
		new PopSecReader(scn, selectedPlanLister2).readFile(popFilePath);
		selectedPlanLister2.printSelectedIndex();
	}
	
}